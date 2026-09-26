import { Injectable, signal } from '@angular/core';
import { UserDto } from '../shared/models/models';

const STORAGE_KEY = 'chubb-claims-current-user-id';

/**
 * SHORTCUT: mock auth. Holds the "logged in" user in memory (and the id in
 * sessionStorage so a page refresh doesn't boot you out mid-demo). Real auth
 * would replace this with a token stored by an OAuth2/OIDC flow, and the
 * ApiService interceptor below would attach it instead of X-User-Id.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<UserDto | null>(null);

  restoreFromStorage(users: UserDto[]): void {
    const savedId = sessionStorage.getItem(STORAGE_KEY);
    if (savedId) {
      const match = users.find((u) => u.id === Number(savedId));
      if (match) this.currentUser.set(match);
    }
  }

  login(user: UserDto): void {
    this.currentUser.set(user);
    sessionStorage.setItem(STORAGE_KEY, String(user.id));
  }

  logout(): void {
    this.currentUser.set(null);
    sessionStorage.removeItem(STORAGE_KEY);
  }
}
