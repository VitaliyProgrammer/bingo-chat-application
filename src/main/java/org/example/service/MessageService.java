package org.example.service;

import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;

public interface MessageService {

    MessageResponseDto sendMessage(MessageRequestDto request);

    MessageResponseDto editMessage(Long messageId, String newContent);

    MessageResponseDto markAsDelivered(Long messageId);

    MessageResponseDto markAsRead(Long messageId);

    void deleteMessage(Long messageId);

    MessagePageResponseDto getChatMessages(Long chatId, int page, int size);
}
