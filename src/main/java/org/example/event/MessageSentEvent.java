package org.example.event;

import java.util.Map;
import org.example.dto.response.MessageResponseDto;

public record MessageSentEvent(
        Long chatId,
        MessageResponseDto message,
        Map<Long, Boolean> participantOnline
) {
}
