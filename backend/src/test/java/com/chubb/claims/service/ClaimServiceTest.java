package com.chubb.claims.service;

import com.chubb.claims.domain.Claim;
import com.chubb.claims.domain.ClaimAttachment;
import com.chubb.claims.domain.ClaimStatus;
import com.chubb.claims.domain.ClaimType;
import com.chubb.claims.domain.InfoRequest;
import com.chubb.claims.domain.User;
import com.chubb.claims.domain.UserRole;
import com.chubb.claims.dto.Dtos.ChangeStatusRequest;
import com.chubb.claims.dto.Dtos.ClaimDetailDto;
import com.chubb.claims.dto.Dtos.InfoRequestRespondRequest;
import com.chubb.claims.event.KafkaEventPublisher;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.web.ForbiddenActionException;
import com.chubb.claims.web.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Service-level rules around assessment and role checks (no Spring context, no DB). */
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock ClaimRepository claimRepository;
    @Mock KafkaEventPublisher eventPublisher;
    @Mock NotificationService notificationService;
    @Spy SlaPolicy slaPolicy = new SlaPolicy(); // real policy with default targets (24h motor, 48h property)

    @InjectMocks ClaimService claimService;

    private User claimant;
    private User officer;
    private Claim claim;

    @BeforeEach
    void setUp() {
        claimant = new User("Amira Hassan", "amira@example.com", UserRole.CLAIMANT);
        claimant.setId(1L);
        officer = new User("Priya Nair", "priya@example.com", UserRole.OFFICER);
        officer.setId(3L);

        claim = new Claim(claimant, ClaimType.MOTOR, LocalDate.of(2026, 8, 1), "Rear-ended at a light.");
        claim.setId(10L);
        claim.setAssignedOfficer(officer);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
    }

    @Test
    void assessingWithoutALiabilityFigureIsRejected() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThrows(ResponseStatusException.class, () ->
                claimService.changeStatus(officer, 10L, new ChangeStatusRequest(ClaimStatus.ASSESSED, null)));

        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        verifyNoInteractions(notificationService, eventPublisher);
    }

    @Test
    void negativeLiabilityIsRejected() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThrows(ResponseStatusException.class, () ->
                claimService.changeStatus(officer, 10L,
                        new ChangeStatusRequest(ClaimStatus.ASSESSED, new BigDecimal("-1"))));

        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
    }

    @Test
    void assessingWithALiabilityFigureSucceeds() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        ClaimDetailDto result = claimService.changeStatus(officer, 10L,
                new ChangeStatusRequest(ClaimStatus.ASSESSED, new BigDecimal("5000")));

        assertEquals(ClaimStatus.ASSESSED, result.status());
        assertEquals(new BigDecimal("5000"), result.estimatedLiability());
    }

    @Test
    void claimantCannotReadTheOfficerQueue() {
        assertThrows(ForbiddenActionException.class, () -> claimService.queue(claimant));
    }

    // ---- attachments: add more, replace, remove ------------------------------

    private ClaimAttachment photoUploadedBy(User uploader, long id) {
        ClaimAttachment photo = new ClaimAttachment(claim, "front-bumper.jpg", "image/jpeg", new byte[]{1, 2, 3}, uploader);
        photo.setId(id);
        claim.getAttachments().add(photo);
        return photo;
    }

    @Test
    void claimantCanAddMorePhotosToTheirOpenClaim() {
        photoUploadedBy(claimant, 1L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.addAttachment(claimant, 10L, "rear-bumper.jpg", "image/jpeg", new byte[]{9});

        assertEquals(2, claim.getAttachments().size());
    }

    @Test
    void uploaderCanReplaceTheirPhoto() {
        ClaimAttachment photo = photoUploadedBy(claimant, 1L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.replaceAttachment(claimant, 10L, 1L, "front-bumper-clearer.jpg", "image/jpeg", new byte[]{7, 7});

        assertEquals("front-bumper-clearer.jpg", photo.getFileName());
        assertArrayEquals(new byte[]{7, 7}, photo.getData());
        assertEquals(2, photo.getSizeBytes());
        assertNotNull(photo.getUpdatedAt());
    }

    @Test
    void uploaderCanRemoveTheirPhoto() {
        photoUploadedBy(claimant, 1L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.deleteAttachment(claimant, 10L, 1L);

        assertTrue(claim.getAttachments().isEmpty());
    }

    @Test
    void nobodyCanRemoveSomeoneElsesFile() {
        photoUploadedBy(claimant, 1L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThrows(ForbiddenActionException.class, () -> claimService.deleteAttachment(officer, 10L, 1L));
        assertEquals(1, claim.getAttachments().size());
    }

    @Test
    void evidenceIsLockedOnceTheClaimIsDecided() {
        photoUploadedBy(claimant, 1L);
        claim.setStatus(ClaimStatus.SETTLED);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThrows(IllegalStateTransitionException.class,
                () -> claimService.addAttachment(claimant, 10L, "late.jpg", "image/jpeg", new byte[]{1}));
        assertThrows(IllegalStateTransitionException.class,
                () -> claimService.replaceAttachment(claimant, 10L, 1L, "new.jpg", "image/jpeg", new byte[]{1}));
        assertThrows(IllegalStateTransitionException.class,
                () -> claimService.deleteAttachment(claimant, 10L, 1L));
    }

    // ---- information requests ------------------------------------------------

    private InfoRequest openQuestion(long id) {
        InfoRequest ir = new InfoRequest(claim, officer, "Please upload a photo of the other car.");
        ir.setId(id);
        claim.getInfoRequests().add(ir);
        return ir;
    }

    @Test
    void answeringTheLastQuestionMovesTheClaimBackToReview() {
        claim.setStatus(ClaimStatus.INFO_REQUESTED);
        openQuestion(1L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.respondToInfoRequest(claimant, 10L, 1L, new InfoRequestRespondRequest("Photo attached."));

        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
    }

    @Test
    void claimStaysInInfoRequestedWhileAnotherQuestionIsOpen() {
        claim.setStatus(ClaimStatus.INFO_REQUESTED);
        openQuestion(1L);
        openQuestion(2L);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.respondToInfoRequest(claimant, 10L, 1L, new InfoRequestRespondRequest("Photo attached."));

        assertEquals(ClaimStatus.INFO_REQUESTED, claim.getStatus());
    }

    // ---- high-value threshold (app.claims.high-value-threshold, default RM 50,000) ----

    @Test
    void assessingAtOrAboveTheThresholdAlertsManagers() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.changeStatus(officer, 10L, new ChangeStatusRequest(ClaimStatus.ASSESSED, new BigDecimal("62000")));

        verify(notificationService).notifyManagers(eq(claim), anyString());
    }

    @Test
    void assessingBelowTheThresholdDoesNotAlertManagers() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        claimService.changeStatus(officer, 10L, new ChangeStatusRequest(ClaimStatus.ASSESSED, new BigDecimal("49999")));

        verify(notificationService, never()).notifyManagers(any(), anyString());
    }

    @Test
    void openClaimsListHighestLiabilityFirstWithUnassessedLast() {
        Claim small = new Claim(claimant, ClaimType.MOTOR, LocalDate.of(2026, 8, 2), "Scratch");
        small.setEstimatedLiability(new BigDecimal("1000"));
        Claim big = new Claim(claimant, ClaimType.PROPERTY, LocalDate.of(2026, 8, 3), "Fire");
        big.setEstimatedLiability(new BigDecimal("80000"));
        when(claimRepository.findByStatusNotIn(any())).thenReturn(List.of(small, claim, big));

        var result = claimService.openClaims(officer);

        assertEquals(new BigDecimal("80000"), result.get(0).estimatedLiability());
        assertEquals(new BigDecimal("1000"), result.get(1).estimatedLiability());
        assertEquals(null, result.get(2).estimatedLiability());
    }

    @Test
    void claimantCannotSeeTheOpenClaimsList() {
        assertThrows(ForbiddenActionException.class, () -> claimService.openClaims(claimant));
    }
}
