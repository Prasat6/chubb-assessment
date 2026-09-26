import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto } from '../shared/models/models';

@Component({
  selector: 'app-officer-queue',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <h3>Unassigned claims</h3>
      <p style="color:var(--text-muted); font-size:13px; margin-top:-8px;">
        Oldest submissions first. Open a claim to assign it to yourself.
      </p>
      <div *ngIf="claims.length === 0" class="empty-state">Queue is empty — nice work.</div>
      <table *ngIf="claims.length">
        <thead><tr><th>ID</th><th>Type</th><th>Claimant</th><th>Incident date</th><th>Submitted</th></tr></thead>
        <tbody>
          <tr *ngFor="let c of claims" class="clickable" (click)="open(c.id)">
            <td>#{{ c.id }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.claimant.name }}</td>
            <td>{{ c.incidentDate }}</td>
            <td>{{ c.createdAt | date: 'short' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class OfficerQueueComponent implements OnInit {
  claims: ClaimSummaryDto[] = [];

  constructor(private api: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.api.officerQueue().subscribe((claims) => (this.claims = claims));
  }

  open(id: number): void {
    this.router.navigate(['/officer/claims', id]);
  }
}
