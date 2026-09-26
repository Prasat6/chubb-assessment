package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "info_request")
@Getter
@Setter
@NoArgsConstructor
public class InfoRequest {

    public enum Status { PENDING, RESPONDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 2000)
    private String response;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant respondedAt;

    public InfoRequest(Claim claim, User requestedBy, String message) {
        this.claim = claim;
        this.requestedBy = requestedBy;
        this.message = message;
    }
}
