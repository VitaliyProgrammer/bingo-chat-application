package org.example.configuration.outbox;

import lombok.RequiredArgsConstructor;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.configuration.rabbitmq.OutboxEventPersistedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutBoxEventRepository outBoxEventRepository;

    private final ApplicationEventPublisher eventPublisher;

    public void publish(OutboxEvent event) {

        OutboxEvent saved = outBoxEventRepository.save(event);

        eventPublisher.publishEvent(new OutboxEventPersistedEvent(saved.getId()));
    }
}
