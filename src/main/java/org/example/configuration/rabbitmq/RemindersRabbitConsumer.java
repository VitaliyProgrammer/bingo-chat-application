package org.example.configuration.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.MessageReminderResponseDto;
import org.example.event.MessageRemindedEvent;
import org.example.service.RedisService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemindersRabbitConsumer {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final RedisService redisService;

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfiguration.REMINDERS_QUEUE)
    public void onMessage(DelayedReminderMessage message) {
        try {
            String key = "event:reminded:" + message.outboxEventId();

            if (!redisService.setIfAbsent(key, "1", IDEMPOTENCY_TTL)) {
                log.debug("Duplicate REMINDED event skipped: {}", key);
                return;
            }

            MessageRemindedEvent reminder = objectMapper.readValue(
                    message.payload(), MessageRemindedEvent.class);

            messagingTemplate.convertAndSendToUser(
                    reminder.userId().toString(),
                    "/queue/reminders",
                    new MessageReminderResponseDto(
                            "REMINDER",
                            reminder.messageId(),
                            reminder.chatId(),
                            reminder.content(),
                            reminder.reminderAt()
                    )
            );

            log.info("Reminder delivered: messageId={}, userId={}",
                    reminder.messageId(), reminder.userId());
        } catch (Exception exception) {
            log.error("Failed to process reminder: outboxEventId={}",
                    message.outboxEventId(), exception);
        }
    }
}
