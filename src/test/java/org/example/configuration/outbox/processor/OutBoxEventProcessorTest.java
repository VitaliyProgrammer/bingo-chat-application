package org.example.configuration.outbox.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.configuration.rabbitmq.DelayedReminderMessage;
import org.example.configuration.rabbitmq.OutboxEventPersistedEvent;
import org.example.configuration.rabbitmq.OutboxMessage;
import org.example.configuration.rabbitmq.RabbitMqConfiguration;
import org.example.configuration.scheduler.SchedulerLockManager;
import org.example.event.MessageRemindedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OutBoxEventProcessorTest {

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SchedulerLockManager schedulerLockManager;

    @Mock
    private ApplicationMetricsService metricsService;

    @InjectMocks
    private OutBoxEventProcessor processor;

    @Test
    void processSingle_firstTime_dispatchesToRabbitAndMarksProcessed() {

        OutboxEvent event = event(1L, OutboxEventStatus.MESSAGE_SENT, "{\"chatId\":1}");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("idempotent:1"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        processor.processSingle(event);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfiguration.OUTBOX_EXCHANGE),
                eq(RabbitMqConfiguration.OUTBOX_ROUTING_KEY),
                captor.capture());

        assertThat(captor.getValue().eventType()).isEqualTo("MESSAGE_SENT");
        assertThat(captor.getValue().payload()).isEqualTo("{\"chatId\":1}");
        assertThat(event.isProcessed()).isTrue();
        verify(outBoxEventRepository).save(event);
    }

    @Test
    void processSingle_duplicate_skipsDispatchButMarksProcessed() {

        OutboxEvent event = event(2L, OutboxEventStatus.MESSAGE_SENT, "{}");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("idempotent:2"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        processor.processSingle(event);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThat(event.isProcessed()).isTrue();
    }

    @Test
    void processSingle_rabbitPublishFails_schedulesRetryWithoutMarkingProcessed() {

        OutboxEvent event = event(3L, OutboxEventStatus.MESSAGE_SENT, "{}");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("idempotent:3"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        processor.processSingle(event);

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
    }

    @Test
    void onOutboxPersisted_unprocessedEvent_dispatchesImmediately() {

        OutboxEvent event = event(4L, OutboxEventStatus.MESSAGE_SENT, "{}");
        when(outBoxEventRepository.findById(4L)).thenReturn(Optional.of(event));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("idempotent:4"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        processor.onOutboxPersisted(new OutboxEventPersistedEvent(4L));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void onOutboxPersisted_alreadyProcessedEvent_doesNothing() {

        OutboxEvent event = event(5L, OutboxEventStatus.MESSAGE_SENT, "{}");
        event.setProcessed(true);
        when(outBoxEventRepository.findById(5L)).thenReturn(Optional.of(event));

        processor.onOutboxPersisted(new OutboxEventPersistedEvent(5L));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void processSingle_reminderEvent_dispatchesToDelayedExchangeWithComputedDelay()
            throws Exception {

        LocalDateTime reminderAt = LocalDateTime.now().plusSeconds(30);
        MessageRemindedEvent reminder = new MessageRemindedEvent(10L, 20L, 30L, "hi", reminderAt);
        String payload = objectMapper.writeValueAsString(reminder);

        OutboxEvent event = event(6L, OutboxEventStatus.MESSAGE_REMINDED, payload);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("idempotent:6"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        processor.processSingle(event);

        ArgumentCaptor<DelayedReminderMessage> messageCaptor =
                ArgumentCaptor.forClass(DelayedReminderMessage.class);
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfiguration.REMINDERS_EXCHANGE),
                eq(RabbitMqConfiguration.REMINDERS_ROUTING_KEY),
                messageCaptor.capture(),
                postProcessorCaptor.capture());

        assertThat(messageCaptor.getValue().outboxEventId()).isEqualTo(6L);
        assertThat(messageCaptor.getValue().payload()).isEqualTo(payload);

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        postProcessorCaptor.getValue().postProcessMessage(amqpMessage);
        long delay = (long) amqpMessage.getMessageProperties().getHeaders().get("x-delay");

        assertThat(delay).isBetween(25_000L, 30_000L);
        assertThat(event.isProcessed()).isTrue();
    }

    private OutboxEvent event(Long id, OutboxEventStatus type, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setEventType(type.name());
        event.setPayload(payload);
        event.setRetryCount(0);
        event.setProcessed(false);
        return event;
    }
}
