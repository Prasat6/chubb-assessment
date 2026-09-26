package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Internal assessment note — never shown to the claimant. */
@Entity
@Table(name = "claim_note")
@Getter
@Setter
@NoArgsConstructor
public class ClaimNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ClaimNote(Claim claim, User author, String content) {
        this.claim = claim;
        this.author = author;
        this.content = content;
    }
}
