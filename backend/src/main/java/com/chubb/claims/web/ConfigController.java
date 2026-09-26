package com.chubb.claims.web;

import com.chubb.claims.service.ClaimService;
import com.chubb.claims.service.SlaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Business settings the frontend needs to display things consistently with the
 * backend, e.g. the high-value threshold used for red highlighting. No user data,
 * so no X-User-Id needed.
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ClaimService claimService;
    private final SlaPolicy slaPolicy;

    @GetMapping
    public Map<String, Object> config() {
        return Map.of(
                "highValueThreshold", claimService.getHighValueThreshold(),
                "slaHours", slaPolicy.targetsByType(),
                "currency", "RM");
    }
}
