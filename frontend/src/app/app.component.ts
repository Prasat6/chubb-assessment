import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { NotificationBellComponent } from './shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  template: `
    <ng-container *ngIf="auth.currentUser() as user; else noShell">
      <div class="topbar">
        <div style="display:flex; align-items:center; gap:14px;">
          <img src="assets/chubb-logo.png" alt="Chubb" style="height:20px; filter:invert(1);" />
          <h1>APAC Claims Platform</h1>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <app-notification-bell></app-notification-bell>
          <span class="user-badge">{{ user.name }} · {{ user.role }}</span>
          <button class="secondary" style="margin:0; padding:6px 12px;" (click)="logout()">Switch user</button>
        </div>
      </div>

      <nav class="tabs">
        <a *ngIf="user.role === 'CLAIMANT'" routerLink="/submit-claim" routerLinkActive="active">Report incident</a>
        <a *ngIf="user.role === 'CLAIMANT'" routerLink="/my-claims" routerLinkActive="active">My claims</a>

        <a *ngIf="user.role === 'OFFICER' || user.role === 'MANAGER'" routerLink="/officer/queue" routerLinkActive="active">Queue</a>
        <a *ngIf="user.role === 'OFFICER' || user.role === 'MANAGER'" routerLink="/officer/workload" routerLinkActive="active">My workload</a>
        <a routerLink="/dashboard" routerLinkActive="active" *ngIf="user.role === 'OFFICER' || user.role === 'MANAGER'">Exposure dashboard</a>
      </nav>

      <div class="page">
        <router-outlet></router-outlet>
      </div>
    </ng-container>

    <ng-template #noShell>
      <router-outlet></router-outlet>
    </ng-template>
  `,
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  logout(): void {
    // Clean SPA navigation instead of a hard page reload: auth.logout() sets
    // currentUser to null synchronously, which immediately tears down
    // everything behind *ngIf="auth.currentUser()" (including the
    // notification bell's polling interval) before we navigate — no window
    // where a leftover request could go out with a stale or missing user.
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
