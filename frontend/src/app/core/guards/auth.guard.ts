import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const token = localStorage.getItem('th_access_token');
  const router = inject(Router);
  if (token) {
    return true;
  }
  return router.createUrlTree(['/login']);
};