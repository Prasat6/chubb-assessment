package com.chubb.claims.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Claim lifecycle state machine.
 *
 * SUBMITTED -> UNDER_REVIEW <-> INFO_REQUESTED
 * UNDER_REVIEW -> ASSESSED -> APPROVED -> SETTLED -> CLOSED
 *                 ASSESSED -> REJECTED -> CLOSED
 *                 ASSESSED -> UNDER_REVIEW (send back for another look)
 *
 * Transitions not listed in ALLOWED are rejected by ClaimService.transitionStatus
 * with a 409 Conflict.
 */
public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    INFO_REQUESTED,
    ASSESSED,
    APPROVED,
    REJECTED,
    SETTLED,
    CLOSED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = Map.of(
            SUBMITTED, EnumSet.of(UNDER_REVIEW),
            UNDER_REVIEW, EnumSet.of(INFO_REQUESTED, ASSESSED),
            INFO_REQUESTED, EnumSet.of(UNDER_REVIEW),
            ASSESSED, EnumSet.of(APPROVED, REJECTED, UNDER_REVIEW),
            APPROVED, EnumSet.of(SETTLED),
            REJECTED, EnumSet.of(CLOSED),
            SETTLED, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(ClaimStatus.class)
    );

    public boolean canTransitionTo(ClaimStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public static boolean isTerminal(ClaimStatus status) {
        return status == SETTLED || status == REJECTED || status == CLOSED;
    }
}
