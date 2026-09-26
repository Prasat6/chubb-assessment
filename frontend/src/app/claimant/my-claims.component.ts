import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClaimSummaryDto } from '../shared/models/models';

@Component({
  selector: 'app-my-claims',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <h3>My claims</h3>
      <div *ngIf="claims.length === 0" class="empty-state">
        You haven't submitted any claims yet.
      </div>
      <table *ngIf="claims.length">
        <thead>
          <tr><th>ID</th><th>Type</th><th>Status</th><th>Incident date</th><th>Last updated</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let c of claims" class="clickable" (click)="open(c.id)">
            <td>#{{ c.id }}</td>
            <td>{{ c.type }}</td>
            <td><span class="badge {{ c.status }}">{{ c.status.replace('_',' ') }}</span></td>
            <td>{{ c.incidentDate }}</td>
            <td>{{ c.updatedAt | date: 'short' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class MyClaimsComponent implements OnInit {
  claims: ClaimSummaryDto[] = [];

  constructor(private api: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.api.myClaims().subscribe((claims) => (this.claims = claims));
  }

  open(id: number): void {
    this.router.navigate(['/my-claims', id]);
  }
}
