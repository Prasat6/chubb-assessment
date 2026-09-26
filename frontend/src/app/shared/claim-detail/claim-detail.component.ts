import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AttachmentDto, ClaimDetailDto, ClaimStatus, UserDto } from '../models/models';
import { ClaimTimelineComponent } from '../claim-timeline/claim-timeline.component';

interface Action {
  key: string;
  label: string;
  style: 'primary' | 'secondary' | 'danger';
}

@Component({
  selector: 'app-claim-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ClaimTimelineComponent],
  template: `
    <div *ngIf="claim" class="card">
      <div style="display:flex; justify-content:space-between; align-items:flex-start;">
        <div>
          <h3 style="margin-bottom:4px;">Claim #{{ claim.id }} — {{ claim.type }}</h3>
          <div style="color:var(--text-muted); font-size:13px;">
            Claimant: {{ claim.claimant.name }} · Incident: {{ claim.incidentDate }}
          </div>
        </div>
        <span class="badge {{ claim.status }}">{{ claim.status.replace('_', ' ') }}</span>
      </div>

      <p style="margin-top:16px;">{{ claim.incidentDescription }}</p>

      <div class="grid" style="margin-top:8px;">
        <div><label>Assigned officer</label>{{ claim.assignedOfficer?.name || '— unassigned —' }}</div>
        <div><label>Estimated liability</label>{{ claim.estimatedLiability != null ? ('RM ' + claim.estimatedLiability) : '— not yet assessed —' }}</div>
      </div>

      <!-- Officer action bar -->
      <div *ngIf="currentUser?.role === 'OFFICER' || currentUser?.role === 'MANAGER'" style="margin-top:18px; border-top:1px solid var(--border); padding-top:14px;">
        <div *ngFor="let action of allowedActions()" style="display:inline-block;">
          <button
            [class.secondary]="action.style === 'secondary'"
            [class.danger]="action.style === 'danger'"
            (click)="startAction(action.key)"
            style="margin-right:8px;"
          >{{ action.label }}</button>
        </div>

        <div *ngIf="pendingAction === 'assess'" style="margin-top:12px;">
          <label>Estimated liability (RM)</label>
          <input type="number" min="0" step="0.01" [(ngModel)]="liabilityInput" placeholder="e.g. 5000" />
          <button (click)="submitAssess()" [disabled]="!canSubmitAssess()">Confirm assessment</button>
        </div>

        <div *ngIf="pendingAction === 'request-info'" style="margin-top:12px;">
          <label>What do you need from the claimant?</label>
          <textarea [(ngModel)]="messageInput" placeholder="e.g. Please attach the police report."></textarea>
          <button (click)="submitInfoRequest()">Send request</button>
        </div>

        <div *ngIf="pendingAction === 'note'" style="margin-top:12px;">
          <label>Internal note (claimant never sees this)</label>
          <textarea [(ngModel)]="messageInput" placeholder="Assessment notes…"></textarea>
          <button (click)="submitNote()">Add note</button>
        </div>
      </div>

      <!-- Info requests -->
      <div *ngIf="claim.infoRequests.length" style="margin-top:20px;">
        <h4>Information requests</h4>
        <div *ngFor="let ir of claim.infoRequests" class="card" style="background:#fbfcfe;">
          <div style="font-size:13px; color:var(--text-muted);">
            Requested by {{ ir.requestedBy.name }} · {{ ir.createdAt | date: 'short' }}
          </div>
          <p style="margin:6px 0;">{{ ir.message }}</p>
          <ng-container *ngIf="ir.status === 'RESPONDED'; else pendingResponse">
            <div style="background:#eef7ee; padding:8px 10px; border-radius:6px; font-size:14px;">
              <strong>Response:</strong> {{ ir.response }}
            </div>
          </ng-container>
          <ng-template #pendingResponse>
            <div *ngIf="currentUser?.role === 'CLAIMANT'; else waitingNote">
              <textarea [(ngModel)]="respondInputs[ir.id]" placeholder="Type your response…"></textarea>
              <button (click)="submitRespond(ir.id)">Submit response</button>
            </div>
            <ng-template #waitingNote>
              <span style="color:var(--text-muted); font-size:13px;">Awaiting claimant response…</span>
            </ng-template>
          </ng-template>
        </div>
      </div>

      <!-- Internal notes (officer/manager only) -->
      <div *ngIf="(currentUser?.role === 'OFFICER' || currentUser?.role === 'MANAGER') && claim.notes.length" style="margin-top:20px;">
        <h4>Internal notes</h4>
        <div *ngFor="let n of claim.notes" style="font-size:14px; padding:8px 0; border-bottom:1px solid var(--border);">
          <div style="color:var(--text-muted); font-size:12px;">{{ n.author.name }} · {{ n.createdAt | date: 'short' }}</div>
          {{ n.content }}
        </div>
      </div>

      <!-- Timeline -->
      <div class="card" style="margin-top:20px; background:#fbfcfe;">
        <h4 style="margin-top:0;">Timeline</h4>
        <app-claim-timeline
          [currentStatus]="claim.status"
          [history]="claim.statusHistory"
          [claimCreatedAt]="claim.createdAt"
        ></app-claim-timeline>
      </div>

      <!-- Attachments: incident photos and documents -->
      <div style="margin-top:20px;">
        <h4>Photos &amp; documents</h4>
        <div *ngIf="claim.attachments.length === 0" class="empty-state" style="padding:8px 0;">No files attached.</div>

        <div class="attachment-grid">
          <div *ngFor="let a of claim.attachments" class="attachment-card">
            <div class="attachment-thumb">
              <img *ngIf="previews[a.id] as src; else fileIcon" [src]="src" [alt]="a.fileName" />
              <ng-template #fileIcon><span class="file-icon">{{ fileExtension(a.fileName) }}</span></ng-template>
            </div>
            <div class="attachment-name" [title]="a.fileName">{{ a.fileName }}</div>
            <div class="attachment-meta">
              {{ formatSize(a.sizeBytes) }} · {{ a.uploadedBy.name }}
              <span *ngIf="a.updatedAt"> · replaced {{ a.updatedAt | date: 'short' }}</span>
            </div>
            <div class="attachment-actions">
              <button class="secondary" (click)="download.emit(a)">Download</button>
              <ng-container *ngIf="canModifyAttachment(a)">
                <input #replaceInput type="file" style="display:none" [accept]="acceptedTypes"
                       (change)="onReplaceSelected($event, a.id)" />
                <button class="secondary" (click)="replaceInput.click()">Replace</button>
                <button class="danger" (click)="confirmRemove(a.id, a.fileName)">Remove</button>
              </ng-container>
            </div>
          </div>
        </div>

        <div *ngIf="canAttach()" style="margin-top:12px;">
          <label>Add more photos or documents (you can select several at once, 10 MB each)</label>
          <input type="file" multiple [accept]="acceptedTypes" (change)="onAttachmentsSelected($event)" />
        </div>
        <div *ngIf="isLocked()" style="margin-top:10px; font-size:13px; color:var(--text-muted);">
          Attachments are locked because this claim is {{ claim.status }}.
        </div>
      </div>
    </div>
  `,
})
export class ClaimDetailComponent implements OnChanges {
  @Input({ required: true }) claim: ClaimDetailDto | null = null;
  @Input({ required: true }) currentUser: UserDto | null = null;

  @Output() assign = new EventEmitter<void>();
  @Output() changeStatus = new EventEmitter<{ status: ClaimStatus; liability?: number }>();
  @Output() requestInfoEvent = new EventEmitter<string>();
  @Output() addNoteEvent = new EventEmitter<string>();
  @Output() respondEvent = new EventEmitter<{ infoRequestId: number; response: string }>();
  @Output() download = new EventEmitter<{ id: number; fileName: string }>();
  /** Object URLs for image attachments, keyed by attachment id (loaded by the page). */
  @Input() previews: Record<number, string> = {};

  @Output() attachFiles = new EventEmitter<File[]>();
  @Output() replaceAttachment = new EventEmitter<{ id: number; file: File }>();
  @Output() removeAttachment = new EventEmitter<number>();

  readonly acceptedTypes = 'image/*,.pdf,.doc,.docx';

  pendingAction: string | null = null;
  liabilityInput: number | null = null;
  messageInput = '';
  respondInputs: Record<number, string> = {};

  ngOnChanges(changes: SimpleChanges): void {
    // Only reset the open form when the claim itself changes, not when image previews arrive.
    if (!changes['claim']) return;
    this.pendingAction = null;
    this.messageInput = '';
    this.liabilityInput = null;
  }

  /**
   * Central place for role/status-based action visibility, kept out of the
   * template so it's a plain function you can unit test in isolation rather
   * than a scatter of *ngIf role checks through the markup.
   */
  allowedActions(): Action[] {
    if (!this.claim || !this.currentUser) return [];
    if (this.currentUser.role === 'CLAIMANT') return [];

    const isAssignedToMe = this.claim.assignedOfficer?.id === this.currentUser.id;
    const actions: Action[] = [];

    if (!this.claim.assignedOfficer) {
      actions.push({ key: 'assign', label: 'Assign to me', style: 'primary' });
      return actions;
    }
    if (!isAssignedToMe) return [];

    switch (this.claim.status) {
      case 'UNDER_REVIEW':
        actions.push({ key: 'request-info', label: 'Request info', style: 'secondary' });
        actions.push({ key: 'assess', label: 'Move to assessed', style: 'primary' });
        actions.push({ key: 'note', label: 'Add note', style: 'secondary' });
        break;
      case 'ASSESSED':
        actions.push({ key: 'approve', label: 'Approve', style: 'primary' });
        actions.push({ key: 'reject', label: 'Reject', style: 'danger' });
        actions.push({ key: 'back-to-review', label: 'Back to review', style: 'secondary' });
        actions.push({ key: 'note', label: 'Add note', style: 'secondary' });
        break;
      case 'APPROVED':
        actions.push({ key: 'settle', label: 'Mark settled', style: 'primary' });
        break;
      case 'INFO_REQUESTED':
        // Without this there was no way out of INFO_REQUESTED in the UI - the backend allows
        // INFO_REQUESTED -> UNDER_REVIEW, but no button offered it, so claims got stuck.
        actions.push({ key: 'back-to-review', label: 'Resume review', style: 'primary' });
        actions.push({ key: 'note', label: 'Add note', style: 'secondary' });
        break;
      default:
        break;
    }
    return actions;
  }

  startAction(key: string): void {
    switch (key) {
      case 'assign':
        this.assign.emit();
        return;
      case 'approve':
        this.changeStatus.emit({ status: 'APPROVED' });
        return;
      case 'reject':
        this.changeStatus.emit({ status: 'REJECTED' });
        return;
      case 'settle':
        this.changeStatus.emit({ status: 'SETTLED' });
        return;
      case 'back-to-review':
        this.changeStatus.emit({ status: 'UNDER_REVIEW' });
        return;
      default:
        this.pendingAction = key; // assess / request-info / note need a form first
    }
  }

  /** The backend requires a non-negative liability figure to assess (it drives the exposure dashboard). */
  canSubmitAssess(): boolean {
    return this.liabilityInput != null && !Number.isNaN(Number(this.liabilityInput)) && Number(this.liabilityInput) >= 0;
  }

  submitAssess(): void {
    if (!this.canSubmitAssess()) return;
    this.changeStatus.emit({ status: 'ASSESSED', liability: Number(this.liabilityInput) });
    this.pendingAction = null;
  }

  submitInfoRequest(): void {
    if (!this.messageInput.trim()) return;
    this.requestInfoEvent.emit(this.messageInput.trim());
    this.pendingAction = null;
  }

  submitNote(): void {
    if (!this.messageInput.trim()) return;
    this.addNoteEvent.emit(this.messageInput.trim());
    this.pendingAction = null;
  }

  submitRespond(infoRequestId: number): void {
    const response = this.respondInputs[infoRequestId];
    if (!response?.trim()) return;
    this.respondEvent.emit({ infoRequestId, response: response.trim() });
  }

  /** Evidence is frozen once a claim is decided (mirrors the backend rule). */
  isLocked(): boolean {
    return !!this.claim && ['SETTLED', 'REJECTED', 'CLOSED'].includes(this.claim.status);
  }

  /** Only the person who uploaded a file may replace or remove it, and only while the claim is open. */
  canModifyAttachment(a: AttachmentDto): boolean {
    return !!this.currentUser && a.uploadedBy.id === this.currentUser.id && !this.isLocked();
  }

  /** Claimant on their own claim, or the assigned officer, may attach evidence. */
  canAttach(): boolean {
    if (!this.claim || !this.currentUser || this.isLocked()) return false;
    if (this.currentUser.role === 'CLAIMANT') return this.claim.claimant.id === this.currentUser.id;
    return this.claim.assignedOfficer?.id === this.currentUser.id;
  }

  onAttachmentsSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    if (files.length) this.attachFiles.emit(files);
    input.value = '';
  }

  onReplaceSelected(event: Event, attachmentId: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.replaceAttachment.emit({ id: attachmentId, file });
    input.value = '';
  }

  confirmRemove(attachmentId: number, fileName: string): void {
    if (window.confirm(`Remove "${fileName}" from this claim?`)) {
      this.removeAttachment.emit(attachmentId);
    }
  }

  fileExtension(fileName: string): string {
    const dot = fileName.lastIndexOf('.');
    return dot >= 0 ? fileName.slice(dot + 1).toUpperCase() : 'FILE';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
