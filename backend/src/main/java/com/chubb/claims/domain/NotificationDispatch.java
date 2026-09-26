package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A simulated send attempt to an external email/SMS provider. There is no
 * real provider wired up (no SMTP/Twilio credentials) — this stands in for
 * one, the same way KafkaEventPublisher stands in for a real broker
 * dependency. Every dispatch is logged here regardless of trigger source
 * (triggeredBy: REST direct call, IN_APP synchronous notification, or KAFKA
 * async consumer) so the log is a single place to see the whole story in a
 * demo — see NotificationDispatchController.
 */
@Entity
@Table(name = "notification_dispatch")
@Getter
@Setter
@NoArgsConstructor
public class NotificationDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DispatchChannel channel;

    @Column(nullable = false)
    private String recipientAddress;

    /** Only meaningful for EMAIL. */
    private String subject;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DispatchStatus status;

    private String providerMessageId;

    /** "REST", "IN_APP", or "KAFKA" — which path triggered this send. */
    @Column(nullable = false)
    private String triggeredBy;

    @Column(nullable = false, updatable = false)
    private Instant dispatchedAt = Instant.now();
}
