package org.example.configuration.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.configuration.scheduler.SchedulerLockManager;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxEventProcessor {

    private static final int BATCH_SIZE = 100;

    private static final int MAX_RETRY_COUNT = 10;

    private static final String LOCK_KEY = "outbox:process";

    private final OutBoxEventRepository outBoxEventRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SchedulerLockManager schedulerLockManager;

    @Scheduled(fixedDelay = 1000)
    public void process() {

        if (!schedulerLockManager.acquireLock(LOCK_KEY, Duration.ofSeconds(10))) {
            log.debug("Skip outbox poll: already running");
            return;
        }
        try {
            List<OutboxEvent> events = outBoxEventRepository
                    .findBatchForProcessing(PageRequest.of(0, BATCH_SIZE));

            log.info("Outbox poll started: fetched={} events", events.size());

            for (OutboxEvent event : events) {
                try {
                    processSingle(event);
                } catch (Exception exception) {
                    log.error("Outbox processing failed: eventId={}", event.getId(), exception);
                }
            }
        } finally {
            schedulerLockManager.releaseLock(LOCK_KEY);
        }
    }

    @Transactional
    public void processSingle(OutboxEvent event) {
        try {
            String key = "idempotent:" + event.getId();
            Boolean firstTime = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofMinutes(10));

            if (Boolean.FALSE.equals(firstTime)) {
                log.debug("Skip duplicate outbox event: eventId={}", event.getId());

                event.setProcessed(true);
                outBoxEventRepository.save(event);
                return;
            }

            dispatch(event);
            event.setProcessed(true);
            outBoxEventRepository.save(event);

            log.debug("Outbox processed: eventId={}", event.getId());
        } catch (Exception exception) {

            int retry = event.getRetryCount() + 1;
            event.setRetryCount(retry);

            if (retry >= MAX_RETRY_COUNT) {
                event.setProcessed(true);
                log.error("Outbox moved to dead state: eventId={}, retries={}",
                        event.getId(), retry, exception);
            } else {
                event.setNextRetryAt(LocalDateTime.now().plusSeconds(5L * retry));
                log.warn("Outbox retry scheduled: eventId={}, retry={}",
                        event.getId(), retry, exception);
            }
            outBoxEventRepository.save(event);
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {

        OutboxEventStatus type = OutboxEventStatus.valueOf(event.getEventType());

        log.debug("Dispatching outbox event: id={}, type={}", event.getId(), event.getEventType());

        switch (type) {

            case MESSAGE_SENT -> {
                MessageSentEvent messageEvent = objectMapper.readValue(
                        event.getPayload(),
                        MessageSentEvent.class
                );
                eventPublisher.publishEvent(messageEvent);
            }

            case MESSAGE_DELIVERED -> {
                MessageDeliveredEvent messageEvent = objectMapper.readValue(
                        event.getPayload(),
                        MessageDeliveredEvent.class
                );
                eventPublisher.publishEvent(messageEvent);
            }

            case MESSAGE_READ -> {
                MessageReadEvent messageEvent = objectMapper.readValue(
                        event.getPayload(),
                        MessageReadEvent.class
                );
                eventPublisher.publishEvent(messageEvent);
            }
            default -> throw new IllegalStateException("Unknown event type!");
        }
    }
}
