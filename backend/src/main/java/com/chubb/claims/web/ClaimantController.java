package com.chubb.claims.web;

import com.chubb.claims.domain.User;
import com.chubb.claims.dto.Dtos.*;
import com.chubb.claims.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimantController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimDetailDto> submit(@CurrentUser User user, @Valid @RequestBody CreateClaimRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.submitClaim(user, req));
    }

    @GetMapping("/mine")
    public List<ClaimSummaryDto> mine(@CurrentUser User user) {
        return claimService.myClaims(user);
    }

    @GetMapping("/{id}")
    public ClaimDetailDto get(@CurrentUser User user, @PathVariable Long id) {
        return claimService.getClaim(user, id);
    }

    @PostMapping("/{id}/info-requests/{infoRequestId}/respond")
    public InfoRequestDto respond(@CurrentUser User user, @PathVariable Long id,
                                   @PathVariable Long infoRequestId,
                                   @Valid @RequestBody InfoRequestRespondRequest req) {
        return claimService.respondToInfoRequest(user, id, infoRequestId, req);
    }
}
