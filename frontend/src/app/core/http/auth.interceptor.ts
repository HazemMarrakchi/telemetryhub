import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('th_access_token');
  const router = inject(Router);
  let request = req;
  if (token) {
    request = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }
  return next(request).pipe(
    catchError((error) => {
      if (error.status === 401) {
        localStorage.removeItem('th_access_token');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};