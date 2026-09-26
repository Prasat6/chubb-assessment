package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claim")
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @ManyToOne
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 2000)
    private String incidentDescription;

    /** Set by an officer during assessment. Null until then. Drives the exposure dashboard. */
    private BigDecimal estimatedLiability;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InfoRequest> infoRequests = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimAttachment> attachments = new ArrayList<>();

    public Claim(User claimant, ClaimType type, LocalDate incidentDate, String incidentDescription) {
        this.claimant = claimant;
        this.type = type;
        this.incidentDate = incidentDate;
        this.incidentDescription = incidentDescription;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
