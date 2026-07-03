package org.example.configuration.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.dto.response.MessageReminderResponseDto;
import org.example.event.MessageReminderEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final int BATCH_SIZE = 50;

    private final OutBoxEventRepository outBoxEventRepository;

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private final SchedulerLockManager schedulerLockManager;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processReminders() {

        boolean acquired = schedulerLockManager.acquireLock(
                "reminder_scheduler",
                Duration.ofSeconds(20)
        );

        if (!acquired) {
            return;
        }

        try {

            List<OutboxEvent> events = outBoxEventRepository.findBatchForProcessing(
                    PageRequest.of(0, BATCH_SIZE)
            );

            for (OutboxEvent event : events) {

                if (!OutboxEventStatus.MESSAGE_REMINDED.name().equals(event.getEventType())) {
                    continue;
                }

                processReminderEvent(event);
            }
        } finally {
            schedulerLockManager.releaseLock("reminder_scheduler");
        }
    }

    public void processReminderEvent(OutboxEvent event) {

        try {
            MessageReminderEvent reminderEvent = objectMapper.readValue(
                    event.getPayload(),
                    MessageReminderEvent.class
            );

            if (reminderEvent.reminderAt().isAfter(LocalDateTime.now())) {
                return;
            }

            messagingTemplate.convertAndSendToUser(
                    reminderEvent.userId().toString(),
                    "/queue/reminders",
                    new MessageReminderResponseDto(
                            "REMINDER",
                            reminderEvent.messageId(),
                            reminderEvent.chatId(),
                            reminderEvent.content(),
                            reminderEvent.reminderAt()
                    )
            );

            event.setProcessed(true);

            log.info("Reminder delivered: messageId={}, userId={}",
                    reminderEvent.messageId(), reminderEvent.userId());

        } catch (Exception exception) {

            event.setRetryCount(event.getRetryCount() + 1);

            event.setNextRetryAt(LocalDateTime.now().plusMinutes(event.getRetryCount() * 2L));

            log.error("Failed to process reminder event: eventId={}", event.getId(), exception);
        }
    }
}
