package org.example.configuration.outbox.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageEditedEvent;
import org.example.event.MessagePinnedEvent;
import org.example.event.MessageReactedEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageRemindedEvent;
import org.example.event.MessageSentEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutBoxEventFactory {

    private static final String AGGREGATE_TYPE_MESSAGE = "MESSAGE";

    private final ObjectMapper objectMapper;

    public OutboxEvent messageSent(MessageSentEvent event) {

        return build(
                event.message().id(),
                OutboxEventStatus.MESSAGE_SENT,
                event
        );
    }

    public OutboxEvent messageDelivered(MessageDeliveredEvent event) {

        return build(
                event.messageId(),
                OutboxEventStatus.MESSAGE_DELIVERED,
                event
        );
    }

    public OutboxEvent messageRead(MessageReadEvent event) {

        return build(
                event.chatId(),
                OutboxEventStatus.MESSAGE_READ,
                event
        );
    }

    public OutboxEvent messageReminded(MessageRemindedEvent event) {

        return build(
                event.chatId(),
                OutboxEventStatus.MESSAGE_REMINDED,
                event
        );
    }

    public OutboxEvent messageEdited(MessageEditedEvent event) {

        return build(
                event.message().id(),
                OutboxEventStatus.MESSAGE_EDITED,
                event
        );
    }

    public OutboxEvent messagePinned(MessagePinnedEvent event) {

        return build(
                event.message().id(),
                OutboxEventStatus.MESSAGE_PINNED,
                event
        );
    }

    public OutboxEvent messageReacted(MessageReactedEvent event) {

        return build(
                event.message().id(),
                OutboxEventStatus.MESSAGE_REACTED,
                event
        );
    }

    private OutboxEvent build(Long aggregateId, OutboxEventStatus status,
                              Object payload) {
        try {
            return OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE_MESSAGE)
                    .aggregateId(aggregateId)
                    .eventType(status.name())
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(LocalDateTime.now())
                    .processed(false)
                    .retryCount(0)
                    .nextRetryAt(null)
                    .build();

        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize outbox event", exception);
        }
    }
}
