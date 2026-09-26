package com.chubb.claims.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Incident evidence (photos, documents) attached to a claim.
 *
 * SHORTCUT: file bytes are stored directly in the relational DB (@Lob) for
 * prototype simplicity - zero extra infra to run locally. In production this
 * would be object storage (S3-compatible), with this row holding a
 * reference/key instead of the bytes themselves - see README.
 */
@Entity
@Table(name = "claim_attachment")
@Getter
@Setter
@NoArgsConstructor
public class ClaimAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    /** Set when the file is replaced with a new version. Null if never replaced. */
    private Instant updatedAt;

    public ClaimAttachment(Claim claim, String fileName, String contentType, byte[] data, User uploadedBy) {
        this.claim = claim;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.uploadedBy = uploadedBy;
    }

    /** Swap in a new version of the file (e.g. a clearer photo). The uploader stays the same. */
    public void replaceContent(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.updatedAt = Instant.now();
    }
}
