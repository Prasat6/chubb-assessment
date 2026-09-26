package com.chubb.claims.repository;

import com.chubb.claims.domain.Claim;
import com.chubb.claims.domain.ClaimStatus;
import com.chubb.claims.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByClaimantOrderByCreatedAtDesc(User claimant);

    List<Claim> findByStatusAndAssignedOfficerIsNullOrderByCreatedAtAsc(ClaimStatus status);

    List<Claim> findByAssignedOfficerOrderByUpdatedAtDesc(User officer);

    List<Claim> findByStatusNotIn(List<ClaimStatus> terminalStatuses);
}
