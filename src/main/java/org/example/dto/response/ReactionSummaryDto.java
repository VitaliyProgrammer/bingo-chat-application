package org.example.dto.response;

import java.util.List;

public record ReactionSummaryDto(
        String emoji,
        int count,
        List<Long> userIds
) {
}
