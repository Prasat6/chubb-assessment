import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const user = auth.currentUser();
  if (!user || req.url.includes('/api/users')) {
    return next(req);
  }
  const cloned = req.clone({ setHeaders: { 'X-User-Id': String(user.id) } });
  return next(cloned);
};
