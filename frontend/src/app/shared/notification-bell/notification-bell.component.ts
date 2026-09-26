import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';
import { AuthService } from '../../auth/auth.service';
import { ApiService } from '../../core/api.service';
import { NotificationDto } from '../models/models';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="position:relative;">
      <button
        style="margin:0; padding:6px 12px; position:relative; background:#FFFFFF; color:#0B2545; border:1px solid #DDE3EA; border-radius:6px; font-size:16px; cursor:pointer;"
        (click)="toggle()">
        🔔
        <span *ngIf="unreadCount > 0" style="
          position:absolute; top:-6px; right:-6px; background:#C8102E; color:#FFFFFF;
          border-radius:10px; font-size:10px; font-weight:700; padding:1px 5px; min-width:16px; text-align:center;">
          {{ unreadCount > 9 ? '9+' : unreadCount }}
        </span>
      </button>

      <div *ngIf="open" style="
        position:absolute; top:calc(100% + 8px); right:0; width:340px; max-height:420px; overflow-y:auto;
        background:#FFFFFF; border:1px solid #DDE3EA; border-radius:10px; box-shadow:0 8px 24px rgba(0,0,0,0.15); z-index:50;">
        <div style="display:flex; justify-content:space-between; align-items:center; padding:12px 14px; border-bottom:1px solid #DDE3EA;">
          <strong style="font-size:13px; color:#0B2545;">Notifications</strong>
          <button
            *ngIf="unreadCount > 0"
            style="margin:0; padding:4px 10px; font-size:11px; background:#FFFFFF; color:#0B2545; border:1px solid #DDE3EA; border-radius:6px; cursor:pointer;"
            (click)="markAllRead()">
            Mark all read
          </button>
        </div>
        <div *ngIf="notifications.length === 0" style="padding:24px 14px; text-align:center; color:#5C6B7A; font-size:13px;">
          Nothing yet.
        </div>
        <div *ngFor="let n of notifications" (click)="openNotification(n)" style="
          padding:12px 14px; border-bottom:1px solid #DDE3EA; cursor:pointer; font-size:13px; color:#1C2733;"
          [style.background]="n.read ? '#FFFFFF' : '#EAF0FA'">
          <div style="display:flex; gap:8px; align-items:flex-start;">
            <span *ngIf="!n.read" style="width:7px; height:7px; border-radius:50%; background:#C8102E; margin-top:5px; flex-shrink:0;"></span>
            <span *ngIf="n.read" style="width:7px; flex-shrink:0;"></span>
            <div>
              <div style="color:#1C2733;">{{ n.message }}</div>
              <div style="color:#5C6B7A; font-size:11px; margin-top:3px;">{{ n.createdAt | date: 'short' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  notifications: NotificationDto[] = [];
  unreadCount = 0;
  open = false;
  private sub?: Subscription;

  constructor(private api: ApiService, private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Poll every 15s - good enough at demo scale, see frontend README. (The backend's
    // /ws/notifications WebSocket is a demo feed of email/SMS dispatches, not per-user.)
    // catchError is inside switchMap on purpose: one failed poll (e.g. backend restarting)
    // used to error the whole stream, and the bell then never updated again.
    this.sub = interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.unreadNotificationCount().pipe(catchError(() => of(null)))),
      )
      .subscribe((res) => {
        if (res) this.unreadCount = res.count;
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  toggle(): void {
    this.open = !this.open;
    if (this.open) {
      this.api.myNotifications().subscribe((list) => (this.notifications = list));
    }
  }

  markAllRead(): void {
    this.api.markAllNotificationsRead().subscribe(() => {
      this.notifications = this.notifications.map((n) => ({ ...n, read: true }));
      this.unreadCount = 0;
    });
  }

  openNotification(n: NotificationDto): void {
    if (!n.read) {
      this.api.markNotificationRead(n.id).subscribe(() => {
        n.read = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      });
    }
    this.open = false;
    const role = this.auth.currentUser()?.role;
    const path = role === 'CLAIMANT' ? ['/my-claims', n.claimId] : ['/officer/claims', n.claimId];
    this.router.navigate(path);
  }
}
