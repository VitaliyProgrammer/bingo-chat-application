package org.example.configuration.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {

    public static final String OUTBOX_EXCHANGE = "outbox.exchange";
    public static final String OUTBOX_QUEUE = "outbox.events";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";

    @Bean
    public DirectExchange outboxExchange() {
        return new DirectExchange(OUTBOX_EXCHANGE);
    }

    @Bean
    public Queue outboxQueue() {
        return new Queue(OUTBOX_QUEUE, true);
    }

    @Bean
    public Binding outboxBinding(Queue outboxQueue, DirectExchange outboxExchange) {
        return BindingBuilder.bind(outboxQueue).to(outboxExchange).with(OUTBOX_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
