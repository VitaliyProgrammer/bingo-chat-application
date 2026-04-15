package org.example.controller.websocket;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessageResponseDto;
import org.example.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(MessageRequestDto request) {

        MessageResponseDto response = messageService.sendMessage(request);

        messagingTemplate.convertAndSend("/topic/chat/" + response.chatId(), response);

        messagingTemplate.convertAndSendToUser(response.senderId().toString(),
                "/queue/delivery", response);
    }
}
