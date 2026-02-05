import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage = 'An error occurred';

            if (error.error instanceof ErrorEvent) {
                // Client-side error
                errorMessage = `Error: ${error.error.message}`;
            } else {
                // Server-side error
                switch (error.status) {
                    case 401:
                        errorMessage = 'Unauthorized - Please login again';
                        // Clear token and redirect to login
                        localStorage.removeItem('access_token');
                        localStorage.removeItem('user_profile');
                        router.navigate(['/login']);
                        break;
                    case 403:
                        errorMessage = 'Forbidden - You don\'t have permission';
                        break;
                    case 404:
                        errorMessage = 'Not Found - Resource not found';
                        break;
                    case 500:
                        errorMessage = 'Internal Server Error';
                        break;
                    default:
                        errorMessage = error.error?.message || `Error Code: ${error.status}`;
                }
            }

            console.error('HTTP Error:', errorMessage);
            return throwError(() => new Error(errorMessage));
        })
    );
};
