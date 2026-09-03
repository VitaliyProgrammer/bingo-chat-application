package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record GroupChatRequestDto(
        @NotBlank(message = "{groupChat.name.notBlank}")
        @Size(max = 100, message = "{groupChat.name.size}")
        String name,

        @NotEmpty(message = "{groupChat.participantIds.notEmpty}")
        @Size(min = 2, max = 100, message = "{groupChat.participantIds.size}")
        Set<Long> participantIds,

        Boolean isPrivate
) {
}
