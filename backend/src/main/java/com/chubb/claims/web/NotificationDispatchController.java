package com.chubb.claims.web;

import com.chubb.claims.domain.DispatchChannel;
import com.chubb.claims.dto.Dtos.DispatchRequest;
import com.chubb.claims.dto.Dtos.DispatchResultDto;
import com.chubb.claims.dto.Dtos.EmailDispatchRequest;
import com.chubb.claims.dto.Dtos.SmsDispatchRequest;
import com.chubb.claims.service.NotificationDispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Deliberately NOT behind @CurrentUser / X-User-Id auth, unlike every other
 * controller in this project. This represents the boundary a real external
 * provider integration would sit behind — it wouldn't authenticate with our
 * internal user session scheme, it'd use its own provider API key. That
 * also makes it trivial to hit directly from SoapUI/Postman with no headers.
 *
 * Two ways to see a dispatch land here:
 *  1. Call one of the endpoints below directly (triggeredBy: REST).
 *  2. Trigger it indirectly — submit/progress a claim in the app, which
 *     calls this synchronously (triggeredBy: IN_APP), and if the optional
 *     Kafka broker is running, the "claim-events" consumer calls it again
 *     (triggeredBy: KAFKA). GET /api/notify/log shows all three.
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationDispatchController {

    private final NotificationDispatchService dispatchService;

    /**
     * Unified endpoint — pick the channel with a field in the body instead
     * of choosing it via URL. Accepts "phone" or "email" as aliases for
     * "to", and "text"/"body" as aliases for "message" — see DispatchRequest.
     * Example: {"channel":"SMS","phone":"+60 12-345 6789","message":"Hi"}
     */
    @PostMapping("/dispatch")
    public DispatchResultDto dispatch(@Valid @RequestBody DispatchRequest req) {
        if (req.channel() == DispatchChannel.EMAIL) {
            return dispatchService.sendEmail(req.to(), req.subject(), req.message(), "REST");
        }
        return dispatchService.sendSms(req.to(), req.message(), "REST");
    }

    @PostMapping("/email")
    public DispatchResultDto email(@Valid @RequestBody EmailDispatchRequest req) {
        return dispatchService.sendEmail(req.to(), req.subject(), req.body(), "REST");
    }

    @PostMapping("/sms")
    public DispatchResultDto sms(@Valid @RequestBody SmsDispatchRequest req) {
        return dispatchService.sendSms(req.to(), req.message(), "REST");
    }

    @GetMapping("/log")
    public List<DispatchResultDto> log() {
        return dispatchService.recent();
    }
}
