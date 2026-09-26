import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AttachmentDto,
  ClaimDetailDto,
  ClaimSummaryDto,
  ClaimStatus,
  ClaimType,
  ClaimNoteDto,
  ExposureDto,
  InfoRequestDto,
  NotificationDto,
  UserDto,
  WorkloadDto,
} from '../shared/models/models';

const BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  // -- users / auth-lite -----------------------------------------------
  listUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${BASE}/users`);
  }

  // -- claimant -----------------------------------------------------------
  submitClaim(payload: { type: ClaimType; incidentDate: string; incidentDescription: string }): Observable<ClaimDetailDto> {
    return this.http.post<ClaimDetailDto>(`${BASE}/claims`, payload);
  }

  myClaims(): Observable<ClaimSummaryDto[]> {
    return this.http.get<ClaimSummaryDto[]>(`${BASE}/claims/mine`);
  }

  getClaim(id: number): Observable<ClaimDetailDto> {
    return this.http.get<ClaimDetailDto>(`${BASE}/claims/${id}`);
  }

  respondToInfoRequest(claimId: number, infoRequestId: number, response: string): Observable<InfoRequestDto> {
    return this.http.post<InfoRequestDto>(
      `${BASE}/claims/${claimId}/info-requests/${infoRequestId}/respond`,
      { response },
    );
  }

  // -- officer -----------------------------------------------------------
  officerQueue(): Observable<ClaimSummaryDto[]> {
    return this.http.get<ClaimSummaryDto[]>(`${BASE}/officer/queue`);
  }

  assignToSelf(claimId: number): Observable<ClaimDetailDto> {
    return this.http.post<ClaimDetailDto>(`${BASE}/officer/claims/${claimId}/assign`, {});
  }

  myWorkload(): Observable<ClaimSummaryDto[]> {
    return this.http.get<ClaimSummaryDto[]>(`${BASE}/officer/claims/mine`);
  }

  workloadSummary(): Observable<WorkloadDto> {
    return this.http.get<WorkloadDto>(`${BASE}/officer/workload-summary`);
  }

  changeStatus(claimId: number, targetStatus: ClaimStatus, estimatedLiability?: number): Observable<ClaimDetailDto> {
    return this.http.post<ClaimDetailDto>(`${BASE}/officer/claims/${claimId}/status`, {
      targetStatus,
      estimatedLiability: estimatedLiability ?? null,
    });
  }

  requestInfo(claimId: number, message: string): Observable<InfoRequestDto> {
    return this.http.post<InfoRequestDto>(`${BASE}/officer/claims/${claimId}/info-requests`, { message });
  }

  addNote(claimId: number, content: string): Observable<ClaimNoteDto> {
    return this.http.post<ClaimNoteDto>(`${BASE}/officer/claims/${claimId}/notes`, { content });
  }

  // -- manager -----------------------------------------------------------
  exposure(): Observable<ExposureDto> {
    return this.http.get<ExposureDto>(`${BASE}/dashboard/exposure`);
  }

  // -- attachments (claimant on own claim, officer on assigned claim) ------
  uploadAttachment(claimId: number, file: File): Observable<AttachmentDto> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<AttachmentDto>(`${BASE}/claims/${claimId}/attachments`, form);
  }

  replaceAttachment(claimId: number, attachmentId: number, file: File): Observable<AttachmentDto> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.put<AttachmentDto>(`${BASE}/claims/${claimId}/attachments/${attachmentId}`, form);
  }

  deleteAttachment(claimId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/claims/${claimId}/attachments/${attachmentId}`);
  }

  downloadAttachment(claimId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${BASE}/claims/${claimId}/attachments/${attachmentId}`, { responseType: 'blob' });
  }

  // -- notifications ------------------------------------------------------
  myNotifications(): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(`${BASE}/notifications`);
  }

  unreadNotificationCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${BASE}/notifications/unread-count`);
  }

  markNotificationRead(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/notifications/${id}/read`, {});
  }

  markAllNotificationsRead(): Observable<void> {
    return this.http.post<void>(`${BASE}/notifications/read-all`, {});
  }
}
