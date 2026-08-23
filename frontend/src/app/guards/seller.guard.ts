import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { ToastrService } from 'ngx-toastr';

export const sellerGuard: CanActivateFn = (route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const toastr = inject(ToastrService);

    const user = auth.getUser();

    // Check if user has SELLER or ADMIN role
    if (user && (user.role === 'SELLER' || user.role === 'ADMIN')) {
        return true;
    }

    toastr.warning('Vui lòng đăng nhập với tài khoản người bán để truy cập');
    router.navigate(['/']);
    return false;
};
