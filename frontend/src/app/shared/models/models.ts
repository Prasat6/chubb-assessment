export type UserRole = 'CLAIMANT' | 'OFFICER' | 'MANAGER';

export interface UserDto {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export type ClaimType = 'MOTOR' | 'PROPERTY';

export type ClaimStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'INFO_REQUESTED'
  | 'ASSESSED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SETTLED'
  | 'CLOSED';

export interface ClaimSummaryDto {
  id: number;
  type: ClaimType;
  status: ClaimStatus;
  incidentDate: string;
  estimatedLiability: number | null;
  createdAt: string;
  updatedAt: string;
  claimant: UserDto;
  assignedOfficer: UserDto | null;
  /** When the claim should be settled or rejected by (submission + SLA hours for its type). */
  dueAt: string;
  slaState: SlaState;
}

/** Resolution-time (SLA) state, computed by the backend's SlaPolicy. */
export type SlaState = 'ON_TRACK' | 'AT_RISK' | 'OVERDUE' | 'MET' | 'MISSED';

export interface InfoRequestDto {
  id: number;
  message: string;
  response: string | null;
  status: 'PENDING' | 'RESPONDED';
  createdAt: string;
  respondedAt: string | null;
  requestedBy: UserDto;
}

export interface ClaimNoteDto {
  id: number;
  content: string;
  createdAt: string;
  author: UserDto;
}

export interface StatusHistoryDto {
  fromStatus: ClaimStatus | null;
  toStatus: ClaimStatus;
  changedAt: string;
  changedBy: UserDto;
}

export interface AttachmentDto {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
  updatedAt: string | null;
  uploadedBy: UserDto;
}

export interface NotificationDto {
  id: number;
  claimId: number;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ClaimDetailDto extends Omit<ClaimSummaryDto, never> {
  incidentDescription: string;
  infoRequests: InfoRequestDto[];
  notes: ClaimNoteDto[];
  statusHistory: StatusHistoryDto[];
  attachments: AttachmentDto[];
  slaTargetHours: number;
  resolvedAt: string | null;
}

export interface WorkloadDto {
  submitted: number;
  underReview: number;
  infoRequested: number;
  assessed: number;
  total: number;
}

export interface TypeBreakdown {
  type: ClaimType;
  count: number;
  totalLiability: number;
}

export interface StatusBreakdownDto {
  status: ClaimStatus;
  count: number;
}

/** Business settings from GET /api/config (e.g. the threshold from application.yml). */
export interface AppConfig {
  highValueThreshold: number;
  /** SLA target in hours per claim type, e.g. { MOTOR: 24, PROPERTY: 48 }. */
  slaHours: Record<string, number>;
  currency: string;
}

export interface ExposureDto {
  totalOutstandingLiability: number;
  openClaims: number;
  byType: TypeBreakdown[];
  byStatus: StatusBreakdownDto[];
  overdueClaims: number;
  atRiskClaims: number;
  slaByType: SlaBreakdown[];
}

export interface SlaBreakdown {
  type: ClaimType;
  targetHours: number;
  onTrack: number;
  atRisk: number;
  overdue: number;
  resolved: number;
  resolvedOnTime: number;
}
