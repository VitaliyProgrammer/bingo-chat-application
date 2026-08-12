package org.example.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushSubscriptionRequestDto(
        @NotBlank
        String endpoint,
        @Valid @NotNull
        Keys keys
) {

    public record Keys(
            @NotBlank
            String p256dh,
            @NotBlank
            String auth
    ) {
    }
}
