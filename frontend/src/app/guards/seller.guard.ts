import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const sellerGuard: CanActivateFn = (route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const user = auth.getUser();

    // Check if user has SELLER or ADMIN role
    if (user && (user.role === 'SELLER' || user.role === 'ADMIN')) {
        return true;
    }

    console.log('User is not a seller, redirecting to home');
    router.navigate(['/']);
    return false;
};
