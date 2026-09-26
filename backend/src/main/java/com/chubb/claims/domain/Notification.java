package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * In-app notification, created synchronously as part of the triggering
 * request (see ClaimService) - NOT sunk behind the Kafka consumer.
 *
 * Why: Kafka publishing is deliberately best-effort (see KafkaEventPublisher)
 * so a broker outage never blocks a claim. But that means if in-app
 * notifications were built only as a Kafka consumer, they'd silently never
 * appear for anyone not running the optional docker-compose Kafka broker -
 * which is most local runs. In-app delivery is core product experience, not
 * a side effect, so it's created directly in the same transaction as the
 * triggering action. The Kafka event stream (claim-events) still exists
 * separately for future *external* channels - email/SMS providers,
 * reporting/audit consumers - where best-effort delivery is the right trade-off.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification(User recipient, Claim claim, String message) {
        this.recipient = recipient;
        this.claim = claim;
        this.message = message;
    }
}
