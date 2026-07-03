package org.example.service;

import java.security.Principal;
import org.example.dto.request.MessageAckRequestDto;
import org.example.dto.request.MessageReminderRequestDto;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;

public interface MessageService {
    void openChat(Long chatId);

    MessageResponseDto sendMessage(MessageRequestDto request);

    MessageResponseDto sendMessage(MessageRequestDto request, Principal principal);

    MessageResponseDto editMessage(Long messageId, String newContent);

    MessageResponseDto markAsDelivered(Long messageId);

    MessageResponseDto markAsRead(Long messageId);

    void markChatAsRead(Long chatId);

    void deleteMessage(Long messageId);

    MessagePageResponseDto getChatMessages(Long chatId, int page, int size);

    void acknowledge(MessageAckRequestDto request);

    MessageResponseDto pinMessage(Long messageId);

    MessageResponseDto setReminder(MessageReminderRequestDto request);
}
