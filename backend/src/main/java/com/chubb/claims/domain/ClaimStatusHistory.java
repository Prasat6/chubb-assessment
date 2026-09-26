package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lightweight persisted audit trail of status transitions. In a system with a
 * durable Kafka log + consumer store this would be derivable from the event
 * stream; persisted directly here so the officer/manager UI has "what
 * happened when" without depending on the broker being up.
 */
@Entity
@Table(name = "claim_status_history")
@Getter
@Setter
@NoArgsConstructor
public class ClaimStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus toStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    public ClaimStatusHistory(Claim claim, ClaimStatus fromStatus, ClaimStatus toStatus, User changedBy) {
        this.claim = claim;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
    }
}
