package com.chubb.claims.event;

import com.chubb.claims.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishing is deliberately best-effort: a notification side-channel must
 * never be able to block or fail a claim state change. If no broker is
 * reachable this logs a warning and the caller's transaction still commits.
 *
 * Publishing is also skipped entirely unless app.kafka.enabled=true (see
 * application.yml). Without that switch, every claim action waited up to
 * 2 seconds (max.block.ms) for broker metadata that never arrived.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public void publish(ClaimEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled - not publishing ClaimEvent for claim {}", event.claimId());
            return;
        }
        try {
            kafkaTemplate.send(KafkaConfig.CLAIM_EVENTS_TOPIC, String.valueOf(event.claimId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish ClaimEvent for claim {} (broker likely unavailable): {}",
                                    event.claimId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            // Covers synchronous failures (e.g. serialization) in addition to the async callback above.
            log.warn("Could not publish ClaimEvent for claim {}: {}", event.claimId(), e.getMessage());
        }
    }
}
