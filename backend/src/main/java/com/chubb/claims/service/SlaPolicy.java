package com.chubb.claims.service;

import com.chubb.claims.domain.Claim;
import com.chubb.claims.domain.ClaimStatus;
import com.chubb.claims.domain.ClaimStatusHistory;
import com.chubb.claims.domain.ClaimType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolution-time targets (SLA) per claim type, set in application.yml under
 * app.claims.sla-hours. A claim is "resolved" when it is SETTLED or REJECTED
 * (or CLOSED); the clock starts when the claim is submitted.
 *
 * The defaults here only apply when the properties are missing (e.g. plain unit tests).
 */
@Component
public class SlaPolicy {

    public enum SlaState {
        /** Open, comfortably within target. */
        ON_TRACK,
        /** Open, less than the at-risk share of the target left. */
        AT_RISK,
        /** Open and past the target. */
        OVERDUE,
        /** Resolved within target. */
        MET,
        /** Resolved, but after the target. */
        MISSED
    }

    @Value("${app.claims.sla-hours.motor:24}")
    private long motorHours = 24;

    @Value("${app.claims.sla-hours.property:48}")
    private long propertyHours = 48;

    /** Share of the target time left at which an open claim is flagged "at risk" (0.25 = last quarter). */
    @Value("${app.claims.sla-at-risk-fraction:0.25}")
    private double atRiskFraction = 0.25;

    public long targetHours(ClaimType type) {
        return type == ClaimType.MOTOR ? motorHours : propertyHours;
    }

    public Map<String, Long> targetsByType() {
        Map<String, Long> targets = new LinkedHashMap<>();
        for (ClaimType t : ClaimType.values()) {
            targets.put(t.name(), targetHours(t));
        }
        return targets;
    }

    public Instant dueAt(Claim claim) {
        return claim.getCreatedAt().plus(Duration.ofHours(targetHours(claim.getType())));
    }

    /** When the claim was first settled or rejected; null while it is still open. */
    public Instant resolvedAt(Claim claim) {
        if (!ClaimStatus.isTerminal(claim.getStatus())) {
            return null;
        }
        return claim.getStatusHistory().stream()
                .filter(h -> h.getToStatus() == ClaimStatus.SETTLED || h.getToStatus() == ClaimStatus.REJECTED)
                .map(ClaimStatusHistory::getChangedAt)
                .min(Instant::compareTo)
                .orElse(claim.getUpdatedAt());
    }

    public SlaState state(Claim claim, Instant now) {
        Instant due = dueAt(claim);
        Instant resolved = resolvedAt(claim);
        if (resolved != null) {
            return resolved.isAfter(due) ? SlaState.MISSED : SlaState.MET;
        }
        if (now.isAfter(due)) {
            return SlaState.OVERDUE;
        }
        long targetSeconds = Duration.ofHours(targetHours(claim.getType())).toSeconds();
        long leftSeconds = Duration.between(now, due).toSeconds();
        return leftSeconds <= targetSeconds * atRiskFraction ? SlaState.AT_RISK : SlaState.ON_TRACK;
    }
}
