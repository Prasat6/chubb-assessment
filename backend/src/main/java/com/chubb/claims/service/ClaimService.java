package com.chubb.claims.service;

import com.chubb.claims.domain.*;
import com.chubb.claims.dto.Dtos.*;
import com.chubb.claims.event.ClaimEvent;
import com.chubb.claims.event.KafkaEventPublisher;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.web.ForbiddenActionException;
import com.chubb.claims.web.IllegalStateTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final KafkaEventPublisher eventPublisher;
    private final NotificationService notificationService;

    /** Claims assessed at or above this liability alert every manager. Prototype-fixed; would be configurable per market in production. */
    private static final java.math.BigDecimal HIGH_VALUE_THRESHOLD = new java.math.BigDecimal("50000");

    // ---- Claimant actions -------------------------------------------------

    public ClaimDetailDto submitClaim(User claimant, CreateClaimRequest req) {
        requireRole(claimant, UserRole.CLAIMANT);
        Claim claim = new Claim(claimant, req.type(), req.incidentDate(), req.incidentDescription());
        claim = claimRepository.save(claim);
        eventPublisher.publish(ClaimEvent.submitted(claim.getId(), claimant.getId()));
        return ClaimDetailDto.from(claim);
    }

    public List<ClaimSummaryDto> myClaims(User claimant) {
        requireRole(claimant, UserRole.CLAIMANT);
        return claimRepository.findByClaimantOrderByCreatedAtDesc(claimant).stream()
                .map(ClaimSummaryDto::from).toList();
    }

    public InfoRequestDto respondToInfoRequest(User claimant, Long claimId, Long infoRequestId,
                                                InfoRequestRespondRequest req) {
        Claim claim = getOwnedClaim(claimant, claimId);
        InfoRequest infoRequest = claim.getInfoRequests().stream()
                .filter(ir -> ir.getId().equals(infoRequestId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info request not found"));
        if (infoRequest.getStatus() == InfoRequest.Status.RESPONDED) {
            throw new IllegalStateTransitionException("This information request has already been responded to");
        }
        infoRequest.setResponse(req.response());
        infoRequest.setStatus(InfoRequest.Status.RESPONDED);
        infoRequest.setRespondedAt(java.time.Instant.now());
        claim.touch();
        if (claim.getAssignedOfficer() != null) {
            notificationService.notify(claim.getAssignedOfficer(), claim,
                    claim.getClaimant().getName() + " responded to your information request on claim #" + claim.getId());
        }
        return InfoRequestDto.from(infoRequest);
    }

    // ---- Officer actions ----------------------------------------------------

    public List<ClaimSummaryDto> queue(User officer) {
        // Was unauthenticated: anyone could list every unassigned claim and its claimant's details.
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        return claimRepository.findByStatusAndAssignedOfficerIsNullOrderByCreatedAtAsc(ClaimStatus.SUBMITTED)
                .stream().map(ClaimSummaryDto::from).toList();
    }

    public ClaimDetailDto assignToSelf(User officer, Long claimId) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        Claim claim = mustFind(claimId);
        if (claim.getAssignedOfficer() != null) {
            throw new IllegalStateTransitionException("Claim is already assigned to " + claim.getAssignedOfficer().getName());
        }
        claim.setAssignedOfficer(officer);
        transitionStatus(claim, ClaimStatus.UNDER_REVIEW, officer);
        return ClaimDetailDto.from(claim);
    }

    public List<ClaimSummaryDto> myWorkload(User officer) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        return claimRepository.findByAssignedOfficerOrderByUpdatedAtDesc(officer).stream()
                .map(ClaimSummaryDto::from).toList();
    }

    public WorkloadDto workloadSummary(User officer) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        List<Claim> claims = claimRepository.findByAssignedOfficerOrderByUpdatedAtDesc(officer);
        long submitted = claims.stream().filter(c -> c.getStatus() == ClaimStatus.SUBMITTED).count();
        long underReview = claims.stream().filter(c -> c.getStatus() == ClaimStatus.UNDER_REVIEW).count();
        long infoRequested = claims.stream().filter(c -> c.getStatus() == ClaimStatus.INFO_REQUESTED).count();
        long assessed = claims.stream().filter(c -> c.getStatus() == ClaimStatus.ASSESSED).count();
        return new WorkloadDto(submitted, underReview, infoRequested, assessed, claims.size());
    }

    public ClaimDetailDto changeStatus(User officer, Long claimId, ChangeStatusRequest req) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        Claim claim = mustFind(claimId);
        assertAssignedTo(claim, officer);
        if (req.estimatedLiability() != null) {
            if (req.estimatedLiability().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estimated liability cannot be negative");
            }
            claim.setEstimatedLiability(req.estimatedLiability());
        }
        // The exposure dashboard sums estimatedLiability, so an ASSESSED claim with
        // no figure silently under-reports exposure. Require one at assessment.
        if (req.targetStatus() == ClaimStatus.ASSESSED && claim.getEstimatedLiability() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter an estimated liability before marking the claim as assessed");
        }
        transitionStatus(claim, req.targetStatus(), officer);
        return ClaimDetailDto.from(claim);
    }

    public InfoRequestDto requestInfo(User officer, Long claimId, InfoRequestCreateRequest req) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        Claim claim = mustFind(claimId);
        assertAssignedTo(claim, officer);
        InfoRequest infoRequest = new InfoRequest(claim, officer, req.message());
        claim.getInfoRequests().add(infoRequest);
        transitionStatus(claim, ClaimStatus.INFO_REQUESTED, officer);
        return InfoRequestDto.from(infoRequest);
    }

    public ClaimNoteDto addNote(User officer, Long claimId, ClaimNoteRequest req) {
        requireRole(officer, UserRole.OFFICER, UserRole.MANAGER);
        Claim claim = mustFind(claimId);
        assertAssignedTo(claim, officer);
        ClaimNote note = new ClaimNote(claim, officer, req.content());
        claim.getNotes().add(note);
        claim.touch();
        return ClaimNoteDto.from(note);
    }

    // ---- Shared read ---------------------------------------------------------

    public ClaimDetailDto getClaim(User requester, Long claimId) {
        Claim claim = mustFind(claimId);
        if (requester.getRole() == UserRole.CLAIMANT && !claim.getClaimant().getId().equals(requester.getId())) {
            throw new ForbiddenActionException("You may only view your own claims");
        }
        return ClaimDetailDto.from(claim);
    }

    // ---- Manager dashboard -----------------------------------------------------

    public ExposureDto exposure() {
        List<Claim> open = claimRepository.findByStatusNotIn(
                List.of(ClaimStatus.SETTLED, ClaimStatus.REJECTED, ClaimStatus.CLOSED));

        var totalLiability = open.stream()
                .map(Claim::getEstimatedLiability)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var byType = java.util.Arrays.stream(ClaimType.values())
                .map(type -> {
                    var forType = open.stream().filter(c -> c.getType() == type).toList();
                    var sum = forType.stream().map(Claim::getEstimatedLiability)
                            .filter(java.util.Objects::nonNull)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    return new TypeBreakdown(type, forType.size(), sum);
                }).toList();

        var byStatus = java.util.Arrays.stream(ClaimStatus.values())
                .filter(s -> !ClaimStatus.isTerminal(s))
                .map(status -> new StatusBreakdown(status, open.stream().filter(c -> c.getStatus() == status).count()))
                .toList();

        return new ExposureDto(totalLiability, open.size(), byType, byStatus);
    }

    // ---- Attachments -----------------------------------------------------

    /**
     * Claimant uploads evidence (e.g. incident photos) for their own claim, or an
     * assigned officer uploads supporting documents during assessment. Basic size
     * guard here; the hard cap is enforced by Spring's multipart config (application.yml).
     */
    public AttachmentDto addAttachment(User uploader, Long claimId, String fileName, String contentType, byte[] data) {
        Claim claim = mustFind(claimId);
        boolean isOwningClaimant = uploader.getRole() == UserRole.CLAIMANT && claim.getClaimant().getId().equals(uploader.getId());
        boolean isAssignedOfficer = (uploader.getRole() == UserRole.OFFICER || uploader.getRole() == UserRole.MANAGER)
                && claim.getAssignedOfficer() != null && claim.getAssignedOfficer().getId().equals(uploader.getId());
        if (!isOwningClaimant && !isAssignedOfficer) {
            throw new ForbiddenActionException("You may only attach files to your own claim, or a claim assigned to you");
        }
        assertEvidenceNotLocked(claim);
        if (data.length == 0) {
            throw new IllegalStateTransitionException("Attached file is empty");
        }
        ClaimAttachment attachment = new ClaimAttachment(claim, fileName, contentType, data, uploader);
        claim.getAttachments().add(attachment);
        claim.touch();
        return AttachmentDto.from(attachment);
    }

    public ClaimAttachment getAttachmentForDownload(User requester, Long claimId, Long attachmentId) {
        Claim claim = mustFind(claimId);
        boolean isOwningClaimant = requester.getRole() == UserRole.CLAIMANT && claim.getClaimant().getId().equals(requester.getId());
        boolean isStaff = requester.getRole() == UserRole.OFFICER || requester.getRole() == UserRole.MANAGER;
        if (!isOwningClaimant && !isStaff) {
            throw new ForbiddenActionException("You may not view attachments on this claim");
        }
        return findAttachment(claim, attachmentId);
    }

    /**
     * Replace a file with a new version (e.g. a clearer photo of the damage).
     * Only the person who uploaded it may replace it, and only while the claim is still open.
     */
    public AttachmentDto replaceAttachment(User requester, Long claimId, Long attachmentId,
                                           String fileName, String contentType, byte[] data) {
        Claim claim = mustFind(claimId);
        ClaimAttachment attachment = findAttachment(claim, attachmentId);
        assertCanModifyAttachment(requester, claim, attachment);
        if (data.length == 0) {
            throw new IllegalStateTransitionException("Attached file is empty");
        }
        attachment.replaceContent(fileName, contentType, data);
        claim.touch();
        return AttachmentDto.from(attachment);
    }

    /** Remove a file. Same rules as replace: uploader only, claim still open. */
    public void deleteAttachment(User requester, Long claimId, Long attachmentId) {
        Claim claim = mustFind(claimId);
        ClaimAttachment attachment = findAttachment(claim, attachmentId);
        assertCanModifyAttachment(requester, claim, attachment);
        claim.getAttachments().remove(attachment); // orphanRemoval deletes the row
        claim.touch();
    }

    // ---- internal helpers -----------------------------------------------------

    private void transitionStatus(Claim claim, ClaimStatus target, User changedBy) {
        ClaimStatus current = claim.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot move claim from " + current + " to " + target);
        }
        claim.getStatusHistory().add(new ClaimStatusHistory(claim, current, target, changedBy));
        claim.setStatus(target);
        claim.touch();
        eventPublisher.publish(ClaimEvent.transitioned(claim.getId(), claim.getClaimant().getId(), current, target));
        notificationService.notify(claim.getClaimant(), claim,
                "Your claim #" + claim.getId() + " moved from " + current + " to " + target);
        if (target == ClaimStatus.ASSESSED && claim.getEstimatedLiability() != null
                && claim.getEstimatedLiability().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            notificationService.notifyManagers(claim,
                    "Claim #" + claim.getId() + " assessed at RM " + claim.getEstimatedLiability()
                            + " - above the RM " + HIGH_VALUE_THRESHOLD + " review threshold");
        }
    }

    private ClaimAttachment findAttachment(Claim claim, Long attachmentId) {
        return claim.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    private void assertCanModifyAttachment(User requester, Claim claim, ClaimAttachment attachment) {
        if (!attachment.getUploadedBy().getId().equals(requester.getId())) {
            throw new ForbiddenActionException("You can only change or remove files you uploaded yourself");
        }
        assertEvidenceNotLocked(claim);
    }

    /** Once a claim is decided (settled, rejected or closed) its evidence is frozen for audit. */
    private void assertEvidenceNotLocked(Claim claim) {
        if (ClaimStatus.isTerminal(claim.getStatus())) {
            throw new IllegalStateTransitionException(
                    "Attachments can't be changed once a claim is " + claim.getStatus());
        }
    }

    private Claim mustFind(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }

    private Claim getOwnedClaim(User claimant, Long claimId) {
        Claim claim = mustFind(claimId);
        if (!claim.getClaimant().getId().equals(claimant.getId())) {
            throw new ForbiddenActionException("You may only act on your own claims");
        }
        return claim;
    }

    private void assertAssignedTo(Claim claim, User officer) {
        if (claim.getAssignedOfficer() == null || !claim.getAssignedOfficer().getId().equals(officer.getId())) {
            throw new ForbiddenActionException("Claim is not assigned to you");
        }
    }

    private void requireRole(User user, UserRole... allowed) {
        for (UserRole r : allowed) {
            if (user.getRole() == r) return;
        }
        throw new ForbiddenActionException("Role " + user.getRole() + " is not permitted to perform this action");
    }
}
