import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Error carrying both a user-presentable `message` and the original response
 * (`status`, `error`), so callers can read `err.message` or `err.error?.message`.
 */
export class ApiError extends Error {
    constructor(message: string, public status: number, public error: any) {
        super(message);
        this.name = 'ApiError';
    }
}

const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh'];

/** Read-only catalogue calls that work anonymously — a stale token must never block them. */
const PUBLIC_READ = /\/(products|categories|brands|flash-sales|templates|marketing)(\/|\?|$)|\/reviews\/product\/|\/analytics\/recommendations\/trending/;

const isAuthEndpoint = (req: HttpRequest<unknown>) => AUTH_ENDPOINTS.some(p => req.url.includes(p));
const isPublicRead = (req: HttpRequest<unknown>) => req.method === 'GET' && PUBLIC_READ.test(req.url);

function clearSession(): void {
    try {
        localStorage.removeItem('access_token');
        localStorage.removeItem('user_profile');
    } catch { /* storage unavailable */ }
}

function friendlyMessage(error: HttpErrorResponse): string {
    const serverMessage = typeof error.error === 'object' ? error.error?.message : undefined;
    if (serverMessage) return serverMessage;

    switch (error.status) {
        case 0:   return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.';
        case 400: return 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.';
        case 401: return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
        case 403: return 'Bạn không có quyền thực hiện thao tác này.';
        case 404: return 'Không tìm thấy nội dung bạn yêu cầu.';
        case 409: return 'Dữ liệu đã thay đổi hoặc bị trùng. Vui lòng tải lại trang và thử lại.';
        case 429: return 'Bạn thao tác quá nhanh. Vui lòng đợi một lát rồi thử lại.';
        default:
            return error.status >= 500
                ? 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.'
                : `Đã xảy ra lỗi (mã ${error.status}).`;
    }
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            if (error.status === 401 && !isAuthEndpoint(req)) {
                // A stale token on an anonymous-friendly call: drop it and retry as a guest.
                if (req.headers.has('Authorization') && isPublicRead(req)) {
                    clearSession();
                    return next(req.clone({ headers: req.headers.delete('Authorization') })).pipe(
                        catchError((retryError: HttpErrorResponse) =>
                            throwError(() => new ApiError(friendlyMessage(retryError), retryError.status, retryError.error)))
                    );
                }

                clearSession();
                const returnUrl = router.url && !router.url.startsWith('/login') ? router.url : undefined;
                router.navigate(['/login'], returnUrl ? { queryParams: { returnUrl } } : {});
            }

            return throwError(() => new ApiError(friendlyMessage(error), error.status, error.error));
        })
    );
};
