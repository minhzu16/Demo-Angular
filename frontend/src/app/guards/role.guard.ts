import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../services/auth.service';

/**
 * Route guard factory: requires a signed-in user holding at least one of `roles`.
 *
 * This is a UX gate only (it hides screens that would just return 403) — the API
 * enforces the real authorization.
 */
export const roleGuard = (...roles: string[]): CanActivateFn => (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toastr = inject(ToastrService);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }

  const userRole = (auth.getUser()?.role || 'BUYER').toUpperCase();
  const allowed = roles.some(r => userRole.split(',').some(u => u.trim().replace(/^ROLE_/, '') === r));
  if (allowed) {
    return true;
  }

  toastr.warning('Tài khoản của bạn không có quyền truy cập khu vực này.');
  return router.createUrlTree(['/home']);
};
