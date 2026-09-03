package org.example.configuration.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.listener.MessageEventListener;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageEditedEvent;
import org.example.event.MessagePinnedEvent;
import org.example.event.MessageReactedEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRabbitConsumer {

    private final MessageEventListener messageEventListener;

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfiguration.OUTBOX_QUEUE)
    public void onMessage(OutboxMessage message) {
        try {
            dispatch(message);
        } catch (Exception exception) {
            log.error("Failed to process outbox message: eventType={}",
                    message.eventType(), exception);

            throw new AmqpRejectAndDontRequeueException(
                    "Outbox message rejected: " + message.eventType(), exception);
        }
    }

    private void dispatch(OutboxMessage message) throws Exception {

        OutboxEventStatus type = OutboxEventStatus.valueOf(message.eventType());

        switch (type) {

            case MESSAGE_SENT -> messageEventListener.handleMessageSent(
                    objectMapper.readValue(message.payload(), MessageSentEvent.class));

            case MESSAGE_DELIVERED -> messageEventListener.handleMessageDelivered(
                    objectMapper.readValue(message.payload(), MessageDeliveredEvent.class));

            case MESSAGE_READ -> messageEventListener.handleMessageRead(
                    objectMapper.readValue(message.payload(), MessageReadEvent.class));

            case MESSAGE_EDITED -> messageEventListener.handleMessageEdited(
                    objectMapper.readValue(message.payload(), MessageEditedEvent.class));

            case MESSAGE_PINNED -> messageEventListener.handleMessagePinned(
                    objectMapper.readValue(message.payload(), MessagePinnedEvent.class));

            case MESSAGE_REACTED -> messageEventListener.handleMessageReacted(
                    objectMapper.readValue(message.payload(), MessageReactedEvent.class));

            default -> throw new IllegalStateException(
                    "Unknown outbox event type: " + message.eventType());
        }
    }
}
