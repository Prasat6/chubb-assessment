package com.chubb.claims.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
        assertTrue(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.ASSESSED));
        assertTrue(ClaimStatus.ASSESSED.canTransitionTo(ClaimStatus.APPROVED));
        assertTrue(ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.SETTLED));
    }

    @Test
    void infoRequestCycleIsAllowedBothWays() {
        assertTrue(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.INFO_REQUESTED));
        assertTrue(ClaimStatus.INFO_REQUESTED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
    }

    @Test
    void assessedCanBounceBackToReview() {
        // e.g. officer realises they need another look before deciding approve/reject
        assertTrue(ClaimStatus.ASSESSED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
    }

    @Test
    void cannotSkipStraightFromSubmittedToApproved() {
        assertFalse(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.APPROVED));
    }

    @Test
    void cannotReopenAClosedClaim() {
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
        assertTrue(ClaimStatus.isTerminal(ClaimStatus.CLOSED));
    }

    @Test
    void rejectedCanOnlyClose() {
        assertTrue(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.CLOSED));
        assertFalse(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.APPROVED));
    }
}
