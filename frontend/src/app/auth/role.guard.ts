import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from '../shared/models/models';

export function roleGuard(allowed: UserRole[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = auth.currentUser();
    if (!user) {
      router.navigateByUrl('/login');
      return false;
    }
    if (!allowed.includes(user.role)) {
      router.navigateByUrl('/login');
      return false;
    }
    return true;
  };
}
