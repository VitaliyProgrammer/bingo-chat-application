package org.example.controller.websocket;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.MessageResponseDto;
import org.example.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageStatusController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/message.delivered")
    public void markAsDelivered(Long messageId) {

        MessageResponseDto response = messageService.markAsDelivered(messageId);

        messagingTemplate.convertAndSend("/topic/chat/" + response.chatId(), response);
    }

    @MessageMapping("/message.read")
    public void markAsRead(Long messageId) {

        MessageResponseDto response = messageService.markAsRead(messageId);

        messagingTemplate.convertAndSend("/topic/chat/" + response.chatId(), response);
    }
}
