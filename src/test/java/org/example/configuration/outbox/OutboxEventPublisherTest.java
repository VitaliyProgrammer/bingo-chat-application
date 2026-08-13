package org.example.configuration.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.outbox.status.OutboxEventStatus;
import org.example.configuration.rabbitmq.OutboxEventPersistedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void publish_regularEvent_savesAndTriggersImmediateDispatch() {

        OutboxEvent event = event(OutboxEventStatus.MESSAGE_SENT, 5L);
        when(outBoxEventRepository.save(event)).thenReturn(event);

        outboxEventPublisher.publish(event);

        verify(eventPublisher).publishEvent(new OutboxEventPersistedEvent(5L));
    }

    @Test
    void publish_reminderEvent_savesWithoutTriggeringImmediateDispatch() {

        OutboxEvent event = event(OutboxEventStatus.MESSAGE_REMINDED, 7L);
        when(outBoxEventRepository.save(event)).thenReturn(event);

        outboxEventPublisher.publish(event);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private OutboxEvent event(OutboxEventStatus type, Long id) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setEventType(type.name());
        return event;
    }
}
