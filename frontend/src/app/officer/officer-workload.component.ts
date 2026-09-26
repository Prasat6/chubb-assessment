import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto, WorkloadDto } from '../shared/models/models';

@Component({
  selector: 'app-officer-workload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stat-row" style="margin-bottom:16px;" *ngIf="summary as s">
      <div class="stat"><div class="value">{{ s.submitted }}</div><div class="label">New / submitted</div></div>
      <div class="stat"><div class="value">{{ s.underReview }}</div><div class="label">Under review</div></div>
      <div class="stat"><div class="value">{{ s.infoRequested }}</div><div class="label">Awaiting claimant</div></div>
      <div class="stat"><div class="value">{{ s.assessed }}</div><div class="label">Assessed</div></div>
      <div class="stat"><div class="value">{{ s.total }}</div><div class="label">Total assigned</div></div>
    </div>

    <div class="card">
      <h3>My workload</h3>
      <div *ngIf="claims.length === 0" class="empty-state">No claims assigned to you yet — check the queue.</div>
      <table *ngIf="claims.length">
        <thead><tr><th>ID</th><th>Type</th><th>Claimant</th><th>Status</th><th>Liability</th><th>Last updated</th></tr></thead>
        <tbody>
          <tr *ngFor="let c of claims" class="clickable" (click)="open(c.id)">
            <td>#{{ c.id }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.claimant.name }}</td>
            <td><span class="badge {{ c.status }}">{{ c.status.replace('_',' ') }}</span></td>
            <td>{{ c.estimatedLiability != null ? ('RM ' + c.estimatedLiability) : '—' }}</td>
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

  constructor(private api: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.api.myWorkload().subscribe((claims) => (this.claims = claims));
    this.api.workloadSummary().subscribe((s) => (this.summary = s));
  }

  open(id: number): void {
    this.router.navigate(['/officer/claims', id]);
  }
}
