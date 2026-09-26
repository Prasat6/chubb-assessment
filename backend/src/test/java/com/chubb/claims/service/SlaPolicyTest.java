package com.chubb.claims.service;

import com.chubb.claims.domain.Claim;
import com.chubb.claims.domain.ClaimStatus;
import com.chubb.claims.domain.ClaimStatusHistory;
import com.chubb.claims.domain.ClaimType;
import com.chubb.claims.domain.User;
import com.chubb.claims.domain.UserRole;
import com.chubb.claims.service.SlaPolicy.SlaState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Resolution-time targets: motor 1 day, property 2 days (defaults, see application.yml). */
class SlaPolicyTest {

    private final SlaPolicy sla = new SlaPolicy();
    private final User claimant = new User("Amira", "amira@example.com", UserRole.CLAIMANT);
    private final Instant submitted = Instant.parse("2026-09-26T09:00:00Z");

    private Claim claim(ClaimType type) {
        Claim c = new Claim(claimant, type, LocalDate.of(2026, 9, 25), "Incident");
        c.setCreatedAt(submitted);
        return c;
    }

    @Test
    void motorIsDueAfterOneDayAndPropertyAfterTwo() {
        assertEquals(submitted.plus(Duration.ofHours(24)), sla.dueAt(claim(ClaimType.MOTOR)));
        assertEquals(submitted.plus(Duration.ofHours(48)), sla.dueAt(claim(ClaimType.PROPERTY)));
    }

    @Test
    void openClaimMovesFromOnTrackToAtRiskToOverdue() {
        Claim motor = claim(ClaimType.MOTOR);
        assertEquals(SlaState.ON_TRACK, sla.state(motor, submitted.plus(Duration.ofHours(10))));
        assertEquals(SlaState.AT_RISK, sla.state(motor, submitted.plus(Duration.ofHours(20)))); // 4h of 24h left
        assertEquals(SlaState.OVERDUE, sla.state(motor, submitted.plus(Duration.ofHours(25))));
    }

    @Test
    void resolvedClaimIsMetOrMissedByWhenItWasSettledOrRejected() {
        Claim onTime = claim(ClaimType.PROPERTY);
        onTime.setStatus(ClaimStatus.SETTLED);
        ClaimStatusHistory settled = new ClaimStatusHistory(onTime, ClaimStatus.APPROVED, ClaimStatus.SETTLED, claimant);
        settled.setChangedAt(submitted.plus(Duration.ofHours(30)));
        onTime.getStatusHistory().add(settled);
        assertEquals(SlaState.MET, sla.state(onTime, submitted.plus(Duration.ofDays(10))));

        Claim late = claim(ClaimType.MOTOR);
        late.setStatus(ClaimStatus.REJECTED);
        ClaimStatusHistory rejected = new ClaimStatusHistory(late, ClaimStatus.ASSESSED, ClaimStatus.REJECTED, claimant);
        rejected.setChangedAt(submitted.plus(Duration.ofHours(30)));
        late.getStatusHistory().add(rejected);
        assertEquals(SlaState.MISSED, sla.state(late, submitted.plus(Duration.ofDays(10))));
    }
}
