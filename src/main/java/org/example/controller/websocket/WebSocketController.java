package org.example.controller.websocket;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ChatAsReadRequestDto;
import org.example.dto.request.MessageAckRequestDto;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.TypingEventResponseDto;
import org.example.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final MessageService messageService;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(MessageRequestDto request, Principal principal) {

        messageService.sendMessage(request, principal);
    }

    @MessageMapping("/chat.typing")
    public void senderUserIsTyping(TypingEventResponseDto response) {

        messagingTemplate.convertAndSend("/topic/chat/" + response.chatId() + "/typing",
                response);
    }

    @MessageMapping("/chat.read")
    public void readChatAsRead(ChatAsReadRequestDto request) {

        messageService.markChatAsRead(request.chatId());
    }

    @MessageMapping("/chat.ack")
    public void acknowledgeMessage(MessageAckRequestDto request) {

        messageService.acknowledge(request);
    }
}
