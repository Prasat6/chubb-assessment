import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto } from '../shared/models/models';
import { SlaBadgeComponent } from '../shared/sla-badge/sla-badge.component';

@Component({
  selector: 'app-officer-queue',
  standalone: true,
  imports: [CommonModule, SlaBadgeComponent],
  template: `
    <div class="card">
      <h3>Unassigned claims</h3>
      <p style="color:var(--text-muted); font-size:13px; margin-top:-8px;">
        Oldest submissions first. Open a claim to assign it to yourself.
      </p>
      <div *ngIf="claims.length === 0" class="empty-state">Queue is empty — nice work.</div>
      <table *ngIf="claims.length">
        <thead><tr><th>ID</th><th>Type</th><th>Claimant</th><th>Incident date</th><th>Submitted</th><th>Resolution target</th></tr></thead>
        <tbody>
          <tr *ngFor="let c of claims" class="clickable" (click)="open(c.id)">
            <td>#{{ c.id }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.claimant.name }}</td>
            <td>{{ c.incidentDate }}</td>
            <td>{{ c.createdAt | date: 'short' }}</td>
            <td><app-sla-badge [state]="c.slaState" [dueAt]="c.dueAt"></app-sla-badge></td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class OfficerQueueComponent implements OnInit {
  claims: ClaimSummaryDto[] = [];

  constructor(private api: ApiService, private router: Router) {}

  /** Lists refresh every 10 seconds so changes made by other users show up without reloading the page. */
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    interval(10000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.officerQueue().pipe(catchError(() => of(null)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((claims) => {
        if (claims) this.claims = claims;
      });
  }

  open(id: number): void {
    this.router.navigate(['/officer/claims', id]);
  }
}
