package org.example.configuration.outbox.processor;

import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.configuration.rabbitmq.OutboxEventPersistedEvent;
import org.example.configuration.rabbitmq.OutboxMessage;
import org.example.configuration.rabbitmq.RabbitMqConfiguration;
import org.example.configuration.scheduler.SchedulerLockManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxEventProcessor {

    private static final int BATCH_SIZE = 100;

    private static final int MAX_RETRY_COUNT = 10;

    private static final String LOCK_KEY = "outbox:process";

    private final OutBoxEventRepository outBoxEventRepository;

    private final RabbitTemplate rabbitTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SchedulerLockManager schedulerLockManager;

    private final ApplicationMetricsService metricsService;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void process() {

        if (!schedulerLockManager.acquireLock(LOCK_KEY, Duration.ofSeconds(10))) {
            log.debug("Skip outbox poll: already running");
            return;
        }

        Timer.Sample sample = metricsService.startOutboxTimer();

        try {
            List<OutboxEvent> events = outBoxEventRepository
                    .findBatchForProcessing(PageRequest.of(0, BATCH_SIZE))
                    .stream()
                    .filter(event -> !OutboxEventStatus.MESSAGE_REMINDED.name()
                            .equals(event.getEventType()))
                    .toList();

            log.info("Outbox poll started: fetched={} events", events.size());

            for (OutboxEvent event : events) {
                try {
                    processSingle(event);
                    metricsService.incrementOutboxProcessed();
                } catch (Exception exception) {

                    metricsService.incrementOutboxFailed();
                    log.error("Outbox processing failed: eventId={}", event.getId(), exception);
                }
            }
        } finally {
            metricsService.stopOutboxTimer(sample);

            schedulerLockManager.releaseLock(LOCK_KEY);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void onOutboxPersisted(OutboxEventPersistedEvent event) {

        outBoxEventRepository.findById(event.outboxEventId())
                .filter(outboxEvent -> !outboxEvent.isProcessed())
                .ifPresent(this::processSingle);
    }

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

    private void dispatch(OutboxEvent event) {

        log.debug("Dispatching outbox event: id={}, type={}", event.getId(), event.getEventType());

        rabbitTemplate.convertAndSend(
                RabbitMqConfiguration.OUTBOX_EXCHANGE,
                RabbitMqConfiguration.OUTBOX_ROUTING_KEY,
                new OutboxMessage(event.getEventType(), event.getPayload())
        );
    }
}
