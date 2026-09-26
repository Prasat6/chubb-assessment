import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';

/**
 * Safety net for auth races (e.g. a request that was already in flight the
 * instant a user switched sessions). Rather than let "Missing X-User-Id
 * header" or "Unknown user" reach a component's error banner, treat any 401
 * as "session's gone — recover" and send the person back to login instead.
 * Registered after authInterceptor in app.config.ts so it sees the response
 * side of the chain closest to the actual HTTP call.
 */
export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err) => {
      if (err?.status === 401) {
        auth.logout();
        router.navigateByUrl('/login');
        return EMPTY; // swallow — we're navigating away, no component needs to render this error
      }
      return throwError(() => err);
    }),
  );
};
