import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto, SlaState, WorkloadDto } from '../shared/models/models';
import { SlaBadgeComponent } from '../shared/sla-badge/sla-badge.component';

@Component({
  selector: 'app-officer-workload',
  standalone: true,
  imports: [CommonModule, SlaBadgeComponent],
  template: `
    <div class="stat-row" style="margin-bottom:16px;" *ngIf="summary as s">
      <div class="stat"><div class="value">{{ s.submitted }}</div><div class="label">New / submitted</div></div>
      <div class="stat"><div class="value">{{ s.underReview }}</div><div class="label">Under review</div></div>
      <div class="stat"><div class="value">{{ s.infoRequested }}</div><div class="label">Awaiting claimant</div></div>
      <div class="stat"><div class="value">{{ s.assessed }}</div><div class="label">Assessed</div></div>
      <div class="stat"><div class="value">{{ s.total }}</div><div class="label">Total assigned</div></div>
      <div class="stat sla-stat OVERDUE"><div class="value">{{ countSla('OVERDUE') }}</div><div class="label">Overdue</div></div>
      <div class="stat sla-stat AT_RISK"><div class="value">{{ countSla('AT_RISK') }}</div><div class="label">At risk</div></div>
    </div>

    <div class="card">
      <h3>My workload</h3>
      <div *ngIf="claims.length === 0" class="empty-state">No claims assigned to you yet — check the queue.</div>
      <table *ngIf="claims.length">
        <thead><tr><th>ID</th><th>Type</th><th>Claimant</th><th>Status</th><th>Resolution target</th><th>Liability</th><th>Last updated</th></tr></thead>
        <tbody>
          <tr *ngFor="let c of claims" class="clickable" [class.high-value]="isHighValue(c)" (click)="open(c.id)">
            <td>#{{ c.id }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.claimant.name }}</td>
            <td><span class="badge {{ c.status }}">{{ c.status.replace('_',' ') }}</span></td>
            <td><app-sla-badge [state]="c.slaState" [dueAt]="c.dueAt"></app-sla-badge></td>
            <td>
              {{ c.estimatedLiability != null ? ('RM ' + (c.estimatedLiability | number: '1.0-0')) : '—' }}
              <span *ngIf="isHighValue(c)" class="hv-tag">HIGH VALUE</span>
            </td>
            <td>{{ c.updatedAt | date: 'short' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class OfficerWorkloadComponent implements OnInit {
  claims: ClaimSummaryDto[] = [];
  summary: WorkloadDto | null = null;
  threshold = 50000;

  constructor(private api: ApiService, private router: Router) {}

  /** Lists refresh every 10 seconds so changes made by other users show up without reloading the page. */
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    interval(10000)
      .pipe(
        startWith(0),
        switchMap(() =>
          forkJoin({
            claims: this.api.myWorkload(),
            summary: this.api.workloadSummary(),
            config: this.api.config(),
          }).pipe(catchError(() => of(null))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        if (!res) return;
        this.claims = res.claims;
        this.summary = res.summary;
        this.threshold = res.config.highValueThreshold;
      });
  }

  countSla(state: SlaState): number {
    return this.claims.filter((c) => c.slaState === state).length;
  }

  /** Same rule as the backend's manager alert: liability at or above app.claims.high-value-threshold. */
  isHighValue(c: ClaimSummaryDto): boolean {
    return c.estimatedLiability != null && c.estimatedLiability >= this.threshold;
  }

  open(id: number): void {
    this.router.navigate(['/officer/claims', id]);
  }
}
