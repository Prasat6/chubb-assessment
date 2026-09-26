import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from './auth.service';
import { UserDto } from '../shared/models/models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-screen">
      <img src="assets/chubb-logo.png" alt="Chubb" style="height:32px; margin-bottom:8px;" />
      <div class="login-card">
        <h2 style="margin-top:0">Chubb APAC Claims</h2>
        <p style="color:var(--text-muted); font-size:14px;">
          Prototype login — pick a demo user. See README for the real-auth swap-in point.
        </p>
        <div *ngIf="users.length === 0" class="empty-state">
          Loading users… (is the backend running on :8080?)
        </div>
        <div *ngFor="let group of grouped">
          <label>{{ group.role }}</label>
          <button
            *ngFor="let u of group.users"
            class="secondary"
            style="width:100%; margin-top:6px; text-align:left;"
            (click)="loginAs(u)"
          >
            {{ u.name }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent implements OnInit {
  users: UserDto[] = [];
  grouped: { role: string; users: UserDto[] }[] = [];

  constructor(private api: ApiService, private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.api.listUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.grouped = ['CLAIMANT', 'OFFICER', 'MANAGER'].map((role) => ({
          role,
          users: users.filter((u) => u.role === role),
        }));
        // Resume a previous demo session (e.g. after a page refresh) instead of forcing re-login.
        this.auth.restoreFromStorage(users);
        const resumed = this.auth.currentUser();
        if (resumed) this.loginAs(resumed);
      },
      error: () => (this.users = []),
    });
  }

  loginAs(user: UserDto): void {
    this.auth.login(user);
    const landing = user.role === 'CLAIMANT' ? '/my-claims' : user.role === 'OFFICER' ? '/officer/queue' : '/dashboard';
    this.router.navigateByUrl(landing);
  }
}
