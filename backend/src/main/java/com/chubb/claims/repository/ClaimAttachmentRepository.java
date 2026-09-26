package com.chubb.claims.repository;

import com.chubb.claims.domain.ClaimAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimAttachmentRepository extends JpaRepository<ClaimAttachment, Long> {
}
