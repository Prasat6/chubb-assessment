import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SlaState } from '../models/models';

/**
 * Shows how a claim is doing against its resolution-time target (SLA):
 * "Due in 5h", "At risk · 3h left", "Overdue by 2h", "Met", "Missed".
 */
@Component({
  selector: 'app-sla-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="sla-badge {{ state }}" [title]="'Due ' + (dueAt | date: 'medium')">{{ label() }}</span>`,
})
export class SlaBadgeComponent {
  @Input({ required: true }) state!: SlaState;
  @Input({ required: true }) dueAt!: string;

  label(): string {
    const diffMs = new Date(this.dueAt).getTime() - Date.now();
    const span = SlaBadgeComponent.humanise(Math.abs(diffMs));
    switch (this.state) {
      case 'ON_TRACK':
        return `Due in ${span}`;
      case 'AT_RISK':
        return `At risk · ${span} left`;
      case 'OVERDUE':
        return `Overdue by ${span}`;
      case 'MET':
        return 'Met target';
      case 'MISSED':
        return 'Missed target';
    }
  }

  static humanise(ms: number): string {
    const minutes = Math.round(ms / 60000);
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h`;
    const days = Math.floor(hours / 24);
    const rest = hours % 24;
    return rest ? `${days}d ${rest}h` : `${days}d`;
  }
}
