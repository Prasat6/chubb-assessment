package com.chubb.claims.web;

import com.chubb.claims.domain.User;
import com.chubb.claims.dto.Dtos.*;
import com.chubb.claims.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerController {

    private final ClaimService claimService;

    @GetMapping("/queue")
    public List<ClaimSummaryDto> queue(@CurrentUser User user) {
        return claimService.queue(user);
    }

    @PostMapping("/claims/{id}/assign")
    public ClaimDetailDto assign(@CurrentUser User user, @PathVariable Long id) {
        return claimService.assignToSelf(user, id);
    }

    @GetMapping("/claims/mine")
    public List<ClaimSummaryDto> myWorkload(@CurrentUser User user) {
        return claimService.myWorkload(user);
    }

    @GetMapping("/workload-summary")
    public WorkloadDto workloadSummary(@CurrentUser User user) {
        return claimService.workloadSummary(user);
    }

    @PostMapping("/claims/{id}/status")
    public ClaimDetailDto changeStatus(@CurrentUser User user, @PathVariable Long id,
                                        @Valid @RequestBody ChangeStatusRequest req) {
        return claimService.changeStatus(user, id, req);
    }

    @PostMapping("/claims/{id}/info-requests")
    public InfoRequestDto requestInfo(@CurrentUser User user, @PathVariable Long id,
                                       @Valid @RequestBody InfoRequestCreateRequest req) {
        return claimService.requestInfo(user, id, req);
    }

    @PostMapping("/claims/{id}/notes")
    public ClaimNoteDto addNote(@CurrentUser User user, @PathVariable Long id,
                                 @Valid @RequestBody ClaimNoteRequest req) {
        return claimService.addNote(user, id, req);
    }
}
