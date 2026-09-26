import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, from, interval } from 'rxjs';
import { concatMap, toArray } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import { ApiService } from '../core/api.service';
import { ClaimDetailComponent } from '../shared/claim-detail/claim-detail.component';
import { ClaimDetailDto, ClaimStatus } from '../shared/models/models';

@Component({
  selector: 'app-claim-detail-page',
  standalone: true,
  imports: [CommonModule, ClaimDetailComponent],
  template: `
    <div *ngIf="error" class="error-banner">{{ error }}</div>
    <app-claim-detail
      *ngIf="claim"
      [claim]="claim"
      [currentUser]="auth.currentUser()"
      (assign)="onAssign()"
      (changeStatus)="onChangeStatus($event)"
      (requestInfoEvent)="onRequestInfo($event)"
      (addNoteEvent)="onAddNote($event)"
      (respondEvent)="onRespond($event)"
      [previews]="previews"
      (download)="onDownload($event)"
      (attachFiles)="onAttachFiles($event)"
      (replaceAttachment)="onReplaceAttachment($event)"
      (removeAttachment)="onRemoveAttachment($event)"
    ></app-claim-detail>
  `,
})
export class ClaimDetailPageComponent implements OnInit, OnDestroy {
  claim: ClaimDetailDto | null = null;
  error = '';
  /** Object URLs for image thumbnails, keyed by attachment id. */
  previews: Record<number, string> = {};
  private claimId!: number;
  private destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    public auth: AuthService,
  ) {}

  ngOnInit(): void {
    // Subscribe to the route instead of reading the snapshot once: Angular reuses this
    // component when only :id changes (e.g. clicking a notification for claim #4 while
    // viewing claim #3), so a one-time snapshot kept showing the old claim.
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.claimId = Number(params.get('id'));
      this.claim = null;
      this.error = '';
      this.load();
    });

    // Pick up changes made by the other party (e.g. the claimant answering while the
    // officer has the claim open) without a manual reload.
    interval(10000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshIfChanged());
  }

  /** Only swaps in the new data if something actually changed, so an open form isn't reset needlessly. */
  private refreshIfChanged(): void {
    if (!this.claim) return;
    const claimId = this.claimId;
    this.api.getClaim(claimId).subscribe({
      next: (c) => {
        if (claimId !== this.claimId || !this.claim) return;
        if (c.updatedAt !== this.claim.updatedAt || c.status !== this.claim.status) {
          this.claim = c;
          this.loadPreviews(c);
        }
      },
      error: () => undefined, // background refresh: stay quiet, the next tick will retry
    });
  }

  ngOnDestroy(): void {
    this.clearPreviews();
  }

  private load(): void {
    this.api.getClaim(this.claimId).subscribe({
      next: (c) => {
        this.claim = c;
        this.loadPreviews(c);
      },
      error: (err) => (this.error = err?.error?.message || 'Could not load claim.'),
    });
  }

  private reportAndReload(obs: Observable<unknown>): void {
    this.error = '';
    obs.subscribe({
      next: () => this.load(),
      error: (err: any) => {
        this.error = err?.error?.message || 'Action failed.';
        this.load(); // e.g. 2 of 3 photos uploaded before one failed: show what did get saved
      },
    });
  }

  onAssign(): void {
    this.reportAndReload(this.api.assignToSelf(this.claimId));
  }

  onChangeStatus(evt: { status: ClaimStatus; liability?: number }): void {
    this.reportAndReload(this.api.changeStatus(this.claimId, evt.status, evt.liability));
  }

  onRequestInfo(message: string): void {
    this.reportAndReload(this.api.requestInfo(this.claimId, message));
  }

  onAddNote(content: string): void {
    this.reportAndReload(this.api.addNote(this.claimId, content));
  }

  onRespond(evt: { infoRequestId: number; response: string }): void {
    this.reportAndReload(this.api.respondToInfoRequest(this.claimId, evt.infoRequestId, evt.response));
  }

  /** Upload several files one after another, then reload once. */
  onAttachFiles(files: File[]): void {
    this.reportAndReload(
      from(files).pipe(
        concatMap((file) => this.api.uploadAttachment(this.claimId, file)),
        toArray(),
      ),
    );
  }

  onReplaceAttachment(evt: { id: number; file: File }): void {
    this.reportAndReload(this.api.replaceAttachment(this.claimId, evt.id, evt.file));
  }

  onRemoveAttachment(attachmentId: number): void {
    this.reportAndReload(this.api.deleteAttachment(this.claimId, attachmentId));
  }

  /**
   * Image thumbnails can't use a plain <img src="/api/..."> because every API call needs
   * the X-User-Id header, so each image is fetched as a blob and shown via an object URL.
   */
  private loadPreviews(claim: ClaimDetailDto): void {
    this.clearPreviews();
    const claimId = claim.id;
    for (const a of claim.attachments) {
      if (!a.contentType.startsWith('image/')) continue;
      this.api.downloadAttachment(claimId, a.id).subscribe({
        next: (blob) => {
          if (this.claimId !== claimId) return; // user navigated to another claim meanwhile
          this.previews = { ...this.previews, [a.id]: URL.createObjectURL(blob) };
        },
        error: () => undefined, // a missing thumbnail isn't worth an error banner
      });
    }
  }

  private clearPreviews(): void {
    Object.values(this.previews).forEach((url) => URL.revokeObjectURL(url));
    this.previews = {};
  }

  onDownload(attachment: { id: number; fileName: string }): void {
    this.api.downloadAttachment(this.claimId, attachment.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = attachment.fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => (this.error = err?.error?.message || 'Could not download attachment.'),
    });
  }
}
