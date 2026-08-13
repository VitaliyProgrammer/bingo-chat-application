package org.example.configuration.rabbitmq;

public record DelayedReminderMessage(Long outboxEventId, String payload) {
}
