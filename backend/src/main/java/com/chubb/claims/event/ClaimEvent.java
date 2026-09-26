package com.chubb.claims.event;

import com.chubb.claims.domain.ClaimStatus;

import java.time.Instant;

/**
 * Published to the "claim-events" topic on submission and every status
 * transition. This is the seam a future notification / reporting service
 * would consume from — see README "Service boundaries".
 */
public record ClaimEvent(
        Long claimId,
        Long claimantId,
        ClaimStatus fromStatus,
        ClaimStatus toStatus,
        Instant occurredAt
) {
    public static ClaimEvent submitted(Long claimId, Long claimantId) {
        return new ClaimEvent(claimId, claimantId, null, ClaimStatus.SUBMITTED, Instant.now());
    }

    public static ClaimEvent transitioned(Long claimId, Long claimantId, ClaimStatus from, ClaimStatus to) {
        return new ClaimEvent(claimId, claimantId, from, to, Instant.now());
    }
}
