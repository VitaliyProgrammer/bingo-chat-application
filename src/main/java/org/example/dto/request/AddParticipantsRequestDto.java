package org.example.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AddParticipantsRequestDto(
        @NotEmpty(message = "{groupChat.participantIds.notEmpty}")
        @Size(min = 1, max = 100, message = "{addParticipants.participantIds.size}")
        Set<Long> participantIds
) {
}
