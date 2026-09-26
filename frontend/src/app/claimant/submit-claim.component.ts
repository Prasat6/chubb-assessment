import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClaimType } from '../shared/models/models';

interface SelectedFile {
  file: File;
  /** Object URL for an image preview; null for non-image files. */
  previewUrl: string | null;
}

@Component({
  selector: 'app-submit-claim',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card" style="max-width:600px;">
      <h3>Report an incident</h3>
      <div *ngIf="error" class="error-banner">{{ error }}</div>

      <label>Claim type</label>
      <select [(ngModel)]="type">
        <option value="MOTOR">Motor</option>
        <option value="PROPERTY">Property</option>
      </select>

      <label>Incident date</label>
      <input type="date" [(ngModel)]="incidentDate" [max]="today" />

      <label>What happened?</label>
      <textarea [(ngModel)]="description" placeholder="Describe the incident in as much detail as you can…"></textarea>

      <label>Photos of the incident / documents (optional, 10 MB each)</label>
      <input type="file" multiple accept="image/*,.pdf,.doc,.docx" (change)="onFilesSelected($event)" />
      <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">
        You can select several files at once, and pick again to add more.
      </div>

      <div *ngIf="selected.length" class="attachment-grid" style="margin-top:10px;">
        <div *ngFor="let s of selected; let i = index" class="attachment-card">
          <div class="attachment-thumb">
            <img *ngIf="s.previewUrl as src; else fileIcon" [src]="src" [alt]="s.file.name" />
            <ng-template #fileIcon><span class="file-icon">{{ extension(s.file.name) }}</span></ng-template>
          </div>
          <div class="attachment-name" [title]="s.file.name">{{ s.file.name }}</div>
          <div class="attachment-actions">
            <button class="danger" [disabled]="submitting" (click)="removeSelected(i)">Remove</button>
          </div>
        </div>
      </div>

      <button (click)="submit()" [disabled]="submitting || !incidentDate || !description.trim()">
        {{ submitting ? (uploadingFiles ? 'Uploading attachments…' : 'Submitting…') : 'Submit claim' }}
      </button>

      <button *ngIf="submittedClaimId && error" class="secondary" (click)="goToClaim()">
        View claim #{{ submittedClaimId }}
      </button>
    </div>
  `,
})
export class SubmitClaimComponent implements OnDestroy {
  type: ClaimType = 'MOTOR';
  incidentDate = '';
  description = '';
  submitting = false;
  uploadingFiles = false;
  error = '';
  today = new Date().toISOString().slice(0, 10);
  selected: SelectedFile[] = [];
  submittedClaimId: number | null = null;

  constructor(private api: ApiService, private router: Router) {}

  /** Adds to the current selection (picking again adds more rather than replacing). */
  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    for (const file of files) {
      const previewUrl = file.type.startsWith('image/') ? URL.createObjectURL(file) : null;
      this.selected.push({ file, previewUrl });
    }
    input.value = ''; // allow re-selecting the same file after removing it
  }

  removeSelected(index: number): void {
    const [removed] = this.selected.splice(index, 1);
    if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl);
  }

  extension(fileName: string): string {
    const dot = fileName.lastIndexOf('.');
    return dot >= 0 ? fileName.slice(dot + 1).toUpperCase() : 'FILE';
  }

  ngOnDestroy(): void {
    this.selected.forEach((s) => s.previewUrl && URL.revokeObjectURL(s.previewUrl));
  }

  submit(): void {
    this.error = '';
    this.submitting = true;
    this.api
      .submitClaim({ type: this.type, incidentDate: this.incidentDate, incidentDescription: this.description.trim() })
      .subscribe({
        next: (claim) => {
          if (this.selected.length === 0) {
            this.router.navigate(['/my-claims', claim.id]);
            return;
          }
          // Claim exists first (so it's never lost if an attachment upload fails),
          // then attachments are uploaded one by one against that claim id.
          this.uploadingFiles = true;
          this.uploadRemaining(claim.id, 0);
        },
        error: (err) => {
          this.submitting = false;
          this.error = err?.error?.message || 'Could not submit claim. Please try again.';
        },
      });
  }

  private uploadRemaining(claimId: number, index: number): void {
    if (index >= this.selected.length) {
      this.router.navigate(['/my-claims', claimId]);
      return;
    }
    this.api.uploadAttachment(claimId, this.selected[index].file).subscribe({
      next: () => this.uploadRemaining(claimId, index + 1),
      error: () => {
        // Claim already exists - don't block the claimant on a failed attachment.
        // Stay on this page so the error is actually visible, offer a manual link instead of auto-navigating.
        this.submitting = false;
        this.uploadingFiles = false;
        this.submittedClaimId = claimId;
        this.error = `Claim submitted, but "${this.selected[index].file.name}" failed to upload. You can retry attaching it from the claim page.`;
      },
    });
  }

  goToClaim(): void {
    if (this.submittedClaimId) this.router.navigate(['/my-claims', this.submittedClaimId]);
  }
}
