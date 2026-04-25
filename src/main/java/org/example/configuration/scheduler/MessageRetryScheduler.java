package org.example.configuration.scheduler;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Message;
import org.example.mapper.MessageMapper;
import org.example.repository.MessageRepository;
import org.example.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryScheduler {
    private final RedisService redisService;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedDelay = 5000)
    public void retryUndeliveredMessages() {

        List<Long> pendingMessages = findPendingMessages();

        for (Long messageId : pendingMessages) {

            String ackKey = "ack:" + messageId;

            if (!redisService.existsKey(ackKey)) {

                log.warn("Retry message: {}", messageId);

                Message message = messageRepository.findById(messageId)
                        .orElse(null);

                if (message == null) {
                    continue;
                }

                MessageResponseDto response = messageMapper.toDto(message);

                messagingTemplate.convertAndSend(
                        "/topic/chat/" + message.getChat().getId(),
                        response
                );
            }
        }
    }

    private List<Long> findPendingMessages() {

        Set<Object> ids = redisTemplate.opsForSet().members("pending:messages");

        if (ids == null) {
            return List.of();
        }

        return ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .toList();
    }
}
