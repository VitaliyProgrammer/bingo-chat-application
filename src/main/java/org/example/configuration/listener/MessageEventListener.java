package org.example.configuration.listener;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.MessageResponseDto;
import org.example.dto.response.UnreadMessagesResponseDto;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.example.service.RedisService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final RedisService redisService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("eventExecutor")
    @EventListener
    public void handleMessageSent(MessageSentEvent event) {

        Long chatId = event.chatId();
        MessageResponseDto message = event.message();

        String key = "event:sent:" + message.id();

        if (!redisService.setIfAbsent(key, "1", IDEMPOTENCY_TTL)) {
            log.debug("Duplicate SENT event skipped: {}", key);
            return;
        }

        log.debug("Event: messages sent -> chatId={}, messageId={}", chatId, message.id());

        messagingTemplate.convertAndSend("/topic/chat/" + event.chatId(), message);

        messagingTemplate.convertAndSendToUser(
                message.senderId().toString(),
                "/queue/delivery",
                message
        );

        event.participantOnline().forEach((userId, isOnline) -> {

            if (userId.equals(message.senderId())) {
                return;
            }
            if (!isOnline) {
                int unreadMessages = redisService.getUnreadMessages(userId, message.chatId());

                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/unread",
                        new UnreadMessagesResponseDto(message.chatId(), unreadMessages)
                );
            }
        });
    }

    @Async("eventExecutor")
    @EventListener
    public void handleMessageDelivered(MessageDeliveredEvent event) {

        String key = "event:delivered" + event.messageId() + ":" + event.userId();

        if (!redisService.setIfAbsent(key, "1", IDEMPOTENCY_TTL)) {
            log.debug("Duplicate DELIVERED event skipped: {}", key);
            return;
        }

        log.debug("Event: messages delivered -> messageId={}, chatId={}",
                event.messageId(), event.userId());

        messagingTemplate.convertAndSend("/topic/chat/" + event,
                Map.of(
                        "type", "DELIVERED",
                        "messageId", event.messageId(),
                        "chatId", event.userId()
                )
        );
    }

    @Async("eventExecutor")
    @EventListener
    public void handleMessageRead(MessageReadEvent event) {

        String key = "event:read:" + event.chatId() + ":" + event.userId();

        if (!redisService.setIfAbsent(key, "1", IDEMPOTENCY_TTL)) {
            log.debug("Duplicate READ event skipped: {}", key);
            return;
        }

        log.debug("Event: messages read -> chatId={}, chatId={}",
                event.chatId(), event.userId());

        messagingTemplate.convertAndSend(
                "/topic/chat/" + event.chatId(),
                Map.of("type", "READ",
                        "chatId", event.chatId(),
                        "chatId", event.userId()
                )
        );
    }
}
