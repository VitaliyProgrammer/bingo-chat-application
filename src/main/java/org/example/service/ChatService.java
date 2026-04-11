package org.example.service;

import java.util.List;
import org.example.dto.response.ChatResponseDto;

public interface ChatService {

    ChatResponseDto createPrivateChat(Long userId);

    List<ChatResponseDto> getMyChats();
}
