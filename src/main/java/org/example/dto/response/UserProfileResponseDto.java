package org.example.dto.response;

import org.example.entity.type.Language;

public record UserProfileResponseDto(
        Long id,
        String firstName,
        String lastName,
        String nickName,
        String email,
        boolean isOnline,
        String avatarUrl,
        int unreadFeedbackReplies,
        Language preferredLanguage
) {
}
