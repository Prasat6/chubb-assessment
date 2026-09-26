package com.chubb.claims.event;

import com.chubb.claims.repository.UserRepository;
import com.chubb.claims.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real EXTERNAL notification channel that a production
 * system would add later — here it calls the same simulated SMS dispatch
 * that /api/notify/sms uses directly (triggeredBy differs: "KAFKA" here vs
 * "REST" there), so GET /api/notify/log shows both paths landing on one
 * capability. In-app notifications (what a claimant, officer, or manager
 * sees inside the platform) are handled separately and synchronously in
 * ClaimService/NotificationService — see Notification.java for why.
 *
 * Only runs when app.kafka.enabled=true (which drives
 * spring.kafka.listener.auto-startup in application.yml) — start the broker
 * with docker-compose.yml first. See README "Optional: Kafka".
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final UserRepository userRepository;
    private final NotificationDispatchService dispatchService;

    @KafkaListener(topics = "claim-events", groupId = "notification-service-stub")
    public void onClaimEvent(ClaimEvent event) {
        String message = event.fromStatus() == null
                ? "Claim #" + event.claimId() + " submitted successfully"
                : "Claim #" + event.claimId() + " moved " + event.fromStatus() + " -> " + event.toStatus();

        log.info("[notification-stub] {} (claimant {})", message, event.claimantId());

        userRepository.findById(event.claimantId()).ifPresent(claimant ->
                dispatchService.sendSms(claimant.getPhone(), message, "KAFKA"));
    }
}

