package org.example.configuration.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxEventProcessor {

    private static final int BATCH_SIZE = 100;

    private static final int MAX_RETRY_COUNT = 10;

    private final OutBoxEventRepository outBoxEventRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void process() {

        List<OutboxEvent> events = outBoxEventRepository
                .findBatchForProcessing(PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : events) {
            try {
                processSingle(event);
            } catch (Exception exception) {

                log.error("Outbox processing failed: eventId={}", event.getId(), exception);
            }
        }
    }

    @Transactional
    public void processSingle(OutboxEvent event) {
        try {
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
