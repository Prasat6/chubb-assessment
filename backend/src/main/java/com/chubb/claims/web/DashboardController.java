package com.chubb.claims.web;

import com.chubb.claims.domain.User;
import com.chubb.claims.dto.Dtos.ClaimSummaryDto;
import com.chubb.claims.dto.Dtos.ExposureDto;
import com.chubb.claims.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Note: not role-gated to MANAGER only — an officer benefiting from seeing
 * overall exposure is reasonable, and there's no sensitive per-claimant PII
 * in the aggregate view. Worth revisiting if the breakdown ever gets
 * per-claim drill-down.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ClaimService claimService;

    @GetMapping("/exposure")
    public ExposureDto exposure() {
        return claimService.exposure();
    }

    /** Open claims, highest liability first. Staff only (includes claimant names). */
    @GetMapping("/open-claims")
    public List<ClaimSummaryDto> openClaims(@CurrentUser User user) {
        return claimService.openClaims(user);
    }
}
