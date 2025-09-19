import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  
  console.log('Auth guard checking authentication...', {
    isAuthenticated: auth.isAuthenticated(),
    token: auth.getToken(),
    targetUrl: state.url
  });
  
  if (auth.isAuthenticated()) {
    console.log('User is authenticated, allowing access');
    return true;
  }
  
  console.log('User not authenticated, redirecting to login');
  router.navigate(['/login']);
  return false;
};
