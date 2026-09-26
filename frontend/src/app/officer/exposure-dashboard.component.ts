import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../core/api.service';
import { ExposureDto } from '../shared/models/models';

@Component({
  selector: 'app-exposure-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="data as d">
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

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.exposure().subscribe((d) => (this.data = d));
  }
}
