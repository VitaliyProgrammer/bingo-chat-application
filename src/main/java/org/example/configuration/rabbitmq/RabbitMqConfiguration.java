package org.example.configuration.rabbitmq;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {

    public static final String OUTBOX_EXCHANGE = "outbox.exchange";
    public static final String OUTBOX_QUEUE = "outbox.events";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";

    public static final String OUTBOX_DEAD_LETTER_EXCHANGE = "outbox.dlx";
    public static final String OUTBOX_DEAD_LETTER_QUEUE = "outbox.events.dlq";
    public static final String OUTBOX_DEAD_LETTER_ROUTING_KEY = "outbox.event.dead";

    public static final String REMINDERS_EXCHANGE = "reminders.delayed.exchange";
    public static final String REMINDERS_QUEUE = "reminders.queue";
    public static final String REMINDERS_ROUTING_KEY = "reminder.due";

    public static final String REMINDERS_DEAD_LETTER_EXCHANGE = "reminders.dlx";
    public static final String REMINDERS_DEAD_LETTER_QUEUE = "reminders.queue.dlq";
    public static final String REMINDERS_DEAD_LETTER_ROUTING_KEY = "reminder.dead";

    @Bean
    public DirectExchange outboxExchange() {
        return new DirectExchange(OUTBOX_EXCHANGE);
    }

    @Bean
    public Queue outboxQueue() {
        return QueueBuilder.durable(OUTBOX_QUEUE)
                .deadLetterExchange(OUTBOX_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(OUTBOX_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding outboxBinding(Queue outboxQueue, DirectExchange outboxExchange) {
        return BindingBuilder.bind(outboxQueue).to(outboxExchange).with(OUTBOX_ROUTING_KEY);
    }

    @Bean
    public DirectExchange outboxDeadLetterExchange() {
        return new DirectExchange(OUTBOX_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue outboxDeadLetterQueue() {
        return QueueBuilder.durable(OUTBOX_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding outboxDeadLetterBinding(Queue outboxDeadLetterQueue,
                                           DirectExchange outboxDeadLetterExchange) {
        return BindingBuilder.bind(outboxDeadLetterQueue)
                .to(outboxDeadLetterExchange)
                .with(OUTBOX_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public CustomExchange remindersDelayedExchange() {
        return new CustomExchange(REMINDERS_EXCHANGE, "x-delayed-message", true, false,
                Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Queue remindersQueue() {
        return QueueBuilder.durable(REMINDERS_QUEUE)
                .deadLetterExchange(REMINDERS_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(REMINDERS_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange remindersDeadLetterExchange() {
        return new DirectExchange(REMINDERS_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue remindersDeadLetterQueue() {
        return QueueBuilder.durable(REMINDERS_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding remindersDeadLetterBinding(Queue remindersDeadLetterQueue,
                                              DirectExchange remindersDeadLetterExchange) {
        return BindingBuilder.bind(remindersDeadLetterQueue)
                .to(remindersDeadLetterExchange)
                .with(REMINDERS_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public Binding remindersBinding(Queue remindersQueue, CustomExchange remindersDelayedExchange) {
        return BindingBuilder.bind(remindersQueue)
                .to(remindersDelayedExchange)
                .with(REMINDERS_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
