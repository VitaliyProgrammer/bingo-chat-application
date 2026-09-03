package org.example.configuration.rabbitmq;

public record OutboxMessage(String eventType, String payload) {
}
