package org.example.configuration.rabbitmq;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.example.configuration.listener.MessageEventListener;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class OutboxRabbitConsumerTest {

    @Mock
    private MessageEventListener messageEventListener;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboxRabbitConsumer consumer;

    @Test
    void onMessage_messageSent_delegatesToMessageEventListener() throws Exception {

        MessageSentEvent event = new MessageSentEvent(1L, null, Map.of());
        OutboxMessage message = new OutboxMessage(
                OutboxEventStatus.MESSAGE_SENT.name(), objectMapper.writeValueAsString(event));

        consumer.onMessage(message);

        verify(messageEventListener).handleMessageSent(event);
    }

    @Test
    void onMessage_messageDelivered_delegatesToMessageEventListener() throws Exception {

        MessageDeliveredEvent event = new MessageDeliveredEvent(1L, 2L, 3L);
        OutboxMessage message = new OutboxMessage(
                OutboxEventStatus.MESSAGE_DELIVERED.name(), objectMapper.writeValueAsString(event));

        consumer.onMessage(message);

        verify(messageEventListener).handleMessageDelivered(event);
    }

    @Test
    void onMessage_messageRead_delegatesToMessageEventListener() throws Exception {

        MessageReadEvent event = new MessageReadEvent(1L, 2L);
        OutboxMessage message = new OutboxMessage(
                OutboxEventStatus.MESSAGE_READ.name(), objectMapper.writeValueAsString(event));

        consumer.onMessage(message);

        verify(messageEventListener).handleMessageRead(event);
    }

    @Test
    void onMessage_unknownEventType_isRejectedToDeadLetterQueue() {

        OutboxMessage message = new OutboxMessage("NOT_A_REAL_TYPE", "{}");

        assertThatThrownBy(() -> consumer.onMessage(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(messageEventListener, never()).handleMessageSent(any());
    }
}
