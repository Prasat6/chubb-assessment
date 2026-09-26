import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ClaimStatus, StatusHistoryDto } from '../models/models';

interface TimelineStep {
  status: ClaimStatus;
  changedAt: string | null;
  changedBy: string | null;
  reached: boolean;
  isCurrent: boolean;
}

@Component({
  selector: 'app-claim-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="timeline">
      <div *ngFor="let step of steps; let last = last" class="timeline-step">
        <div class="timeline-marker">
          <div class="dot" [class.reached]="step.reached" [class.current]="step.isCurrent"></div>
          <div *ngIf="!last" class="line" [class.reached]="step.reached"></div>
        </div>
        <div class="timeline-content">
          <div class="timeline-label" [class.reached]="step.reached">{{ step.status.replace('_', ' ') }}</div>
          <div *ngIf="step.changedAt" class="timeline-meta">{{ step.changedAt | date: 'short' }} · {{ step.changedBy }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .timeline { display: flex; flex-direction: column; }
    .timeline-step { display: flex; }
    .timeline-marker { display: flex; flex-direction: column; align-items: center; margin-right: 14px; }
    .dot {
      width: 14px; height: 14px; border-radius: 50%;
      background: #dde3ea; border: 2px solid #dde3ea; flex-shrink: 0;
    }
    .dot.reached { background: var(--chubb-navy); border-color: var(--chubb-navy); }
    .dot.current { background: var(--chubb-red); border-color: var(--chubb-red); box-shadow: 0 0 0 4px rgba(200,16,46,0.15); }
    .line { width: 2px; flex: 1; min-height: 24px; background: #dde3ea; }
    .line.reached { background: var(--chubb-navy); }
    .timeline-content { padding-bottom: 18px; }
    .timeline-label { font-size: 13px; font-weight: 600; color: var(--text-muted); }
    .timeline-label.reached { color: var(--text); }
    .timeline-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
  `],
})
export class ClaimTimelineComponent {
  @Input({ required: true }) currentStatus!: ClaimStatus;
  @Input({ required: true }) history: StatusHistoryDto[] = [];
  @Input({ required: true }) claimCreatedAt!: string;

  // Canonical happy-path sequence shown regardless of which branch a claim actually took
  // (a rejected claim just won't show APPROVED/SETTLED as reached) - keeps the timeline
  // predictable to scan rather than dynamically reshaping itself per claim.
  private static readonly SEQUENCE: ClaimStatus[] = [
    'SUBMITTED', 'UNDER_REVIEW', 'ASSESSED', 'APPROVED', 'SETTLED',
  ];

  get steps(): TimelineStep[] {
    const currentIndex = ClaimTimelineComponent.SEQUENCE.indexOf(this.currentStatus);
    const isOffPath = this.currentStatus === 'INFO_REQUESTED' || this.currentStatus === 'REJECTED' || this.currentStatus === 'CLOSED';

    return ClaimTimelineComponent.SEQUENCE.map((status, index) => {
      const historyEntry = [...this.history].reverse().find((h) => h.toStatus === status);
      const reached = isOffPath
        ? index <= this.reachedIndexForOffPathStatus()
        : currentIndex >= 0 && index <= currentIndex;
      return {
        status,
        changedAt: historyEntry?.changedAt ?? (status === 'SUBMITTED' ? this.claimCreatedAt : null),
        changedBy: historyEntry?.changedBy?.name ?? null,
        reached,
        isCurrent: status === this.currentStatus,
      };
    });
  }

  /** For INFO_REQUESTED/REJECTED/CLOSED, which aren't in the main sequence, find how far along the happy path history actually got. */
  private reachedIndexForOffPathStatus(): number {
    let furthest = 0;
    for (const h of this.history) {
      const idx = ClaimTimelineComponent.SEQUENCE.indexOf(h.toStatus);
      if (idx > furthest) furthest = idx;
    }
    return furthest;
  }
}
