import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin, interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto, ExposureDto } from '../shared/models/models';
import { SlaBadgeComponent } from '../shared/sla-badge/sla-badge.component';

@Component({
  selector: 'app-exposure-dashboard',
  standalone: true,
  imports: [CommonModule, SlaBadgeComponent],
  template: `
    <div *ngIf="data as d">
      <!-- Resolution-time (SLA) overview -->
      <div class="card">
        <h3>Resolution time (SLA)</h3>
        <p style="color:var(--text-muted); font-size:13px; margin-top:-8px;">
          Target from submission to settlement or rejection:
          <ng-container *ngFor="let t of d.slaByType; let last = last">
            <strong>{{ t.type }}</strong> {{ formatTarget(t.targetHours) }}<span *ngIf="!last"> · </span>
          </ng-container>
          (set in application.yml).
        </p>
        <div class="stat-row" style="margin-bottom:16px;">
          <div class="stat sla-stat OVERDUE">
            <div class="value">{{ d.overdueClaims }}</div>
            <div class="label">Overdue</div>
          </div>
          <div class="stat sla-stat AT_RISK">
            <div class="value">{{ d.atRiskClaims }}</div>
            <div class="label">At risk (due soon)</div>
          </div>
          <div class="stat sla-stat ON_TRACK">
            <div class="value">{{ totalOnTrack(d) }}</div>
            <div class="label">On track</div>
          </div>
          <div class="stat">
            <div class="value">{{ onTimeRate(d) }}</div>
            <div class="label">Resolved on time</div>
          </div>
        </div>
        <table>
          <thead>
            <tr><th>Type</th><th>Target</th><th>On track</th><th>At risk</th><th>Overdue</th><th>Resolved on time</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let t of d.slaByType">
              <td>{{ t.type }}</td>
              <td>{{ formatTarget(t.targetHours) }}</td>
              <td>{{ t.onTrack }}</td>
              <td [class.sla-cell-warn]="t.atRisk > 0">{{ t.atRisk }}</td>
              <td [class.sla-cell-bad]="t.overdue > 0">{{ t.overdue }}</td>
              <td>{{ t.resolvedOnTime }} / {{ t.resolved }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="stat-row" style="margin-bottom:16px;">
        <div class="stat">
          <div class="value">RM {{ d.totalOutstandingLiability | number: '1.0-0' }}</div>
          <div class="label">Total outstanding liability</div>
        </div>
        <div class="stat">
          <div class="value">{{ d.openClaims }}</div>
          <div class="label">Open claims</div>
        </div>
      </div>

      <div class="card">
        <h3>Open claims by liability</h3>
        <p style="color:var(--text-muted); font-size:13px; margin-top:-8px;">
          Highest first. Rows in <span class="high-value-legend">red</span> are at or above the
          RM {{ threshold | number: '1.0-0' }} high-value threshold. Click a row to open the claim.
        </p>
        <div *ngIf="claims.length === 0" class="empty-state">No open claims.</div>
        <table *ngIf="claims.length">
          <thead><tr><th>ID</th><th>Type</th><th>Claimant</th><th>Status</th><th>Officer</th><th>Resolution target</th><th>Liability</th></tr></thead>
          <tbody>
            <tr *ngFor="let c of claims" class="clickable" [class.high-value]="isHighValue(c)" (click)="open(c.id)">
              <td>#{{ c.id }}</td>
              <td>{{ c.type }}</td>
              <td>{{ c.claimant.name }}</td>
              <td><span class="badge {{ c.status }}">{{ c.status.replace('_',' ') }}</span></td>
              <td>{{ c.assignedOfficer?.name || '—' }}</td>
              <td><app-sla-badge [state]="c.slaState" [dueAt]="c.dueAt"></app-sla-badge></td>
              <td>
                {{ c.estimatedLiability != null ? ('RM ' + (c.estimatedLiability | number: '1.0-0')) : 'Not assessed' }}
                <span *ngIf="isHighValue(c)" class="hv-tag">HIGH VALUE</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>By claim type</h3>
        <table>
          <thead><tr><th>Type</th><th>Open claims</th><th>Liability</th></tr></thead>
          <tbody>
            <tr *ngFor="let t of d.byType">
              <td>{{ t.type }}</td>
              <td>{{ t.count }}</td>
              <td>RM {{ t.totalLiability | number: '1.0-0' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>By status</h3>
        <table>
          <thead><tr><th>Status</th><th>Count</th></tr></thead>
          <tbody>
            <tr *ngFor="let s of d.byStatus">
              <td><span class="badge {{ s.status }}">{{ s.status.replace('_',' ') }}</span></td>
              <td>{{ s.count }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class ExposureDashboardComponent implements OnInit {
  data: ExposureDto | null = null;
  claims: ClaimSummaryDto[] = [];
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
            exposure: this.api.exposure(),
            claims: this.api.openClaims(),
            config: this.api.config(),
          }).pipe(catchError(() => of(null))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        if (!res) return;
        this.data = res.exposure;
        this.claims = res.claims;
        this.threshold = res.config.highValueThreshold;
      });
  }

  formatTarget(hours: number): string {
    return hours % 24 === 0 ? `${hours / 24} day${hours === 24 ? '' : 's'}` : `${hours}h`;
  }

  totalOnTrack(d: ExposureDto): number {
    return d.slaByType.reduce((sum, t) => sum + t.onTrack, 0);
  }

  /** Share of resolved claims that met their target, e.g. "3 / 4 (75%)". */
  onTimeRate(d: ExposureDto): string {
    const resolved = d.slaByType.reduce((sum, t) => sum + t.resolved, 0);
    const onTime = d.slaByType.reduce((sum, t) => sum + t.resolvedOnTime, 0);
    return resolved === 0 ? '—' : `${Math.round((onTime / resolved) * 100)}%`;
  }

  isHighValue(c: ClaimSummaryDto): boolean {
    return c.estimatedLiability != null && c.estimatedLiability >= this.threshold;
  }

  open(id: number): void {
    this.router.navigate(['/officer/claims', id]);
  }
}
