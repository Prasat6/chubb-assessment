package com.chubb.claims.service;

import com.chubb.claims.domain.DispatchChannel;
import com.chubb.claims.domain.DispatchStatus;
import com.chubb.claims.domain.NotificationDispatch;
import com.chubb.claims.dto.Dtos.DispatchResultDto;
import com.chubb.claims.event.NotificationWebSocketHandler;
import com.chubb.claims.repository.NotificationDispatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Simulated external email/SMS provider — no real SMTP/SMS credentials are
 * wired up. Every call here stands in for what would otherwise be a call to
 * something like SES, SendGrid, or Twilio: it "sends" (always succeeds
 * unless the recipient address is blank), logs it, and records it in
 * NotificationDispatch so it shows up in GET /api/notify/log regardless of
 * which of the three paths triggered it (REST direct call, IN_APP
 * synchronous notification, KAFKA async consumer — see NotificationService
 * and NotificationListener). It also pushes the same event over
 * NotificationWebSocketHandler — GET /api/notify/log is pull (ask and get an
 * answer), the WebSocket is push (arrives the moment it happens, no
 * re-polling) — see SOAPUI_TESTING.md.
 */
@Service
@RequiredArgsConstructor
// REQUIRES_NEW: each dispatch gets its own transaction. NotificationService.notify()
// wraps the dispatch call in try/catch so a dispatch hiccup "never breaks the claim
// action" - but with the default REQUIRED propagation an exception in here still
// marked the caller's (the claim action's) transaction rollback-only, so the claim
// change failed anyway with UnexpectedRollbackException. Separate transaction = the
// try/catch actually protects the claim action.
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationDispatchRepository repository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public DispatchResultDto sendEmail(String to, String subject, String body, String triggeredBy) {
        return dispatch(DispatchChannel.EMAIL, to, subject, body, triggeredBy);
    }

    public DispatchResultDto sendSms(String to, String message, String triggeredBy) {
        return dispatch(DispatchChannel.SMS, to, null, message, triggeredBy);
    }

    public List<DispatchResultDto> recent() {
        return repository.findAllByOrderByDispatchedAtDesc().stream().map(DispatchResultDto::from).toList();
    }

    private DispatchResultDto dispatch(DispatchChannel channel, String to, String subject, String body, String triggeredBy) {
        NotificationDispatch d = new NotificationDispatch();
        d.setChannel(channel);
        d.setRecipientAddress(to == null ? "" : to);
        d.setSubject(subject);
        d.setMessage(body);
        d.setTriggeredBy(triggeredBy);

        if (to == null || to.isBlank()) {
            d.setStatus(DispatchStatus.FAILED);
            log.warn("[notify-dispatch] {} FAILED via {} — no recipient address on file", channel, triggeredBy);
        } else {
            d.setStatus(DispatchStatus.SENT);
            d.setProviderMessageId("sim-" + UUID.randomUUID());
            log.info("[notify-dispatch] {} -> {} via {}: \"{}\"", channel, to, triggeredBy, body);
        }
        repository.save(d);
        DispatchResultDto dto = DispatchResultDto.from(d);
        broadcastBestEffort(dto);
        return dto;
    }

    private void broadcastBestEffort(DispatchResultDto dto) {
        try {
            webSocketHandler.broadcast(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            // A WS push failure (e.g. no clients, serialization edge case) must never affect the
            // dispatch record itself — same best-effort philosophy as KafkaEventPublisher.
            log.warn("[ws] broadcast failed for dispatch {}: {}", dto.id(), e.getMessage());
        }
    }
}
