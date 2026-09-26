package com.chubb.claims.dto;

import com.chubb.claims.domain.*;
import com.chubb.claims.service.SlaPolicy;
import com.chubb.claims.service.SlaPolicy.SlaState;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** All request/response DTOs collected in one file to keep the prototype's surface area easy to scan. */
public class Dtos {

    public record UserDto(Long id, String name, String email, UserRole role) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole());
        }
    }

    public record CreateClaimRequest(
            @NotNull ClaimType type,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotBlank String incidentDescription
    ) {}

    public record ChangeStatusRequest(
            @NotNull ClaimStatus targetStatus,
            BigDecimal estimatedLiability // optional, typically set when moving to ASSESSED
    ) {}

    public record InfoRequestCreateRequest(@NotBlank String message) {}

    public record InfoRequestRespondRequest(@NotBlank String response) {}

    public record ClaimNoteRequest(@NotBlank String content) {}

    public record InfoRequestDto(
            Long id, String message, String response, InfoRequest.Status status,
            Instant createdAt, Instant respondedAt, UserDto requestedBy
    ) {
        public static InfoRequestDto from(InfoRequest r) {
            return new InfoRequestDto(r.getId(), r.getMessage(), r.getResponse(), r.getStatus(),
                    r.getCreatedAt(), r.getRespondedAt(), UserDto.from(r.getRequestedBy()));
        }
    }

    public record ClaimNoteDto(Long id, String content, Instant createdAt, UserDto author) {
        public static ClaimNoteDto from(ClaimNote n) {
            return new ClaimNoteDto(n.getId(), n.getContent(), n.getCreatedAt(), UserDto.from(n.getAuthor()));
        }
    }

    /** Metadata only - never carries the file bytes. Download is a separate endpoint/request. */
    public record AttachmentDto(Long id, String fileName, String contentType, long sizeBytes, Instant uploadedAt,
                                Instant updatedAt, UserDto uploadedBy) {
        public static AttachmentDto from(ClaimAttachment a) {
            return new AttachmentDto(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
                    a.getUploadedAt(), a.getUpdatedAt(), UserDto.from(a.getUploadedBy()));
        }
    }

    public record NotificationDto(Long id, Long claimId, String message, boolean read, Instant createdAt) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getClaim().getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
        }
    }

    public record EmailDispatchRequest(
            @NotBlank @JsonAlias({"email", "phone"}) String to,
            @NotBlank String subject,
            @NotBlank String body
    ) {}

    public record SmsDispatchRequest(
            @NotBlank @JsonAlias({"phone", "to"}) String to,
            @NotBlank @JsonAlias({"text", "body"}) String message
    ) {}

    /**
     * Unified endpoint request — you pick the channel explicitly instead of
     * choosing it via which URL you call. "phone" works as an alias for "to"
     * here regardless of channel (an email send just ignores the name and
     * uses whatever string you put there as the address).
     */
    public record DispatchRequest(
            @NotNull DispatchChannel channel,
            @NotBlank @JsonAlias({"phone", "email"}) String to,
            String subject,
            @NotBlank @JsonAlias({"text", "body"}) String message
    ) {}

    public record DispatchResultDto(
            Long id, DispatchChannel channel, String recipientAddress, String subject, String message,
            DispatchStatus status, String providerMessageId, String triggeredBy, Instant dispatchedAt
    ) {
        public static DispatchResultDto from(NotificationDispatch d) {
            return new DispatchResultDto(d.getId(), d.getChannel(), d.getRecipientAddress(), d.getSubject(),
                    d.getMessage(), d.getStatus(), d.getProviderMessageId(), d.getTriggeredBy(), d.getDispatchedAt());
        }
    }

    public record StatusHistoryDto(ClaimStatus fromStatus, ClaimStatus toStatus, Instant changedAt, UserDto changedBy) {
        public static StatusHistoryDto from(ClaimStatusHistory h) {
            return new StatusHistoryDto(h.getFromStatus(), h.getToStatus(), h.getChangedAt(), UserDto.from(h.getChangedBy()));
        }
    }

    /** Slim shape for list views (queue, mine, workload) — no nested collections. */
    public record ClaimSummaryDto(
            Long id, ClaimType type, ClaimStatus status, LocalDate incidentDate,
            BigDecimal estimatedLiability, Instant createdAt, Instant updatedAt,
            UserDto claimant, UserDto assignedOfficer,
            Instant dueAt, SlaState slaState
    ) {
        public static ClaimSummaryDto from(Claim c, SlaPolicy sla) {
            return new ClaimSummaryDto(
                    c.getId(), c.getType(), c.getStatus(), c.getIncidentDate(), c.getEstimatedLiability(),
                    c.getCreatedAt(), c.getUpdatedAt(), UserDto.from(c.getClaimant()),
                    c.getAssignedOfficer() == null ? null : UserDto.from(c.getAssignedOfficer()),
                    sla.dueAt(c), sla.state(c, Instant.now())
            );
        }
    }

    /** Full shape for the claim detail view. */
    public record ClaimDetailDto(
            Long id, ClaimType type, ClaimStatus status, LocalDate incidentDate, String incidentDescription,
            BigDecimal estimatedLiability, Instant createdAt, Instant updatedAt,
            UserDto claimant, UserDto assignedOfficer,
            List<InfoRequestDto> infoRequests, List<ClaimNoteDto> notes, List<StatusHistoryDto> statusHistory,
            List<AttachmentDto> attachments,
            Instant dueAt, SlaState slaState, long slaTargetHours, Instant resolvedAt
    ) {
        public static ClaimDetailDto from(Claim c, SlaPolicy sla) {
            return new ClaimDetailDto(
                    c.getId(), c.getType(), c.getStatus(), c.getIncidentDate(), c.getIncidentDescription(),
                    c.getEstimatedLiability(), c.getCreatedAt(), c.getUpdatedAt(),
                    UserDto.from(c.getClaimant()),
                    c.getAssignedOfficer() == null ? null : UserDto.from(c.getAssignedOfficer()),
                    c.getInfoRequests().stream().map(InfoRequestDto::from).toList(),
                    c.getNotes().stream().map(ClaimNoteDto::from).toList(),
                    c.getStatusHistory().stream().map(StatusHistoryDto::from).toList(),
                    c.getAttachments().stream().map(AttachmentDto::from).toList(),
                    sla.dueAt(c), sla.state(c, Instant.now()), sla.targetHours(c.getType()), sla.resolvedAt(c)
            );
        }
    }

    public record WorkloadDto(long submitted, long underReview, long infoRequested, long assessed, long total) {}

    public record ExposureDto(
            BigDecimal totalOutstandingLiability,
            long openClaims,
            List<TypeBreakdown> byType,
            List<StatusBreakdown> byStatus,
            long overdueClaims,
            long atRiskClaims,
            List<SlaBreakdown> slaByType
    ) {}

    /**
     * Resolution-time performance per claim type: open claims by SLA state, and
     * for resolved claims how many met the target.
     */
    public record SlaBreakdown(ClaimType type, long targetHours,
                               long onTrack, long atRisk, long overdue,
                               long resolved, long resolvedOnTime) {}

    public record TypeBreakdown(ClaimType type, long count, BigDecimal totalLiability) {}

    public record StatusBreakdown(ClaimStatus status, long count) {}
}
