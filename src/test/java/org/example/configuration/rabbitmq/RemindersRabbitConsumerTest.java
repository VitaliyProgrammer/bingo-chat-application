package org.example.configuration.rabbitmq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.LocalDateTime;
import org.example.dto.response.MessageReminderResponseDto;
import org.example.event.MessageRemindedEvent;
import org.example.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RemindersRabbitConsumerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private RemindersRabbitConsumer consumer;

    @Test
    void onMessage_firstTime_sendsReminderToUser() throws Exception {

        MessageRemindedEvent reminder = new MessageRemindedEvent(
                1L, 2L, 3L, "don't forget", LocalDateTime.now());
        String payload = objectMapper.writeValueAsString(reminder);

        when(redisService.setIfAbsent(eq("event:reminded:9"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        consumer.onMessage(new DelayedReminderMessage(9L, payload));

        verify(messagingTemplate).convertAndSendToUser(
                eq("2"), eq("/queue/reminders"), any(MessageReminderResponseDto.class));
    }

    @Test
    void onMessage_duplicate_skipsSend() {

        when(redisService.setIfAbsent(eq("event:reminded:10"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        consumer.onMessage(new DelayedReminderMessage(10L, "{}"));

        verify(messagingTemplate, never()).convertAndSendToUser(
                any(), any(), any(MessageReminderResponseDto.class));
    }
}
