package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.GroupModerationRequestDto;
import org.example.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Chat API", description = "Group chat moderation for administrators")
public class AdminChatController {

    private final ChatService chatService;

    @PostMapping("/{chatId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block group chat",
            description = "Admin-only: freezes a group chat - "
                    + "messages and group management actions are rejected until unblocked")
    public void blockGroupChat(@PathVariable Long chatId,
                              @Valid @RequestBody GroupModerationRequestDto request) {

        chatService.blockGroupChat(chatId, request.reason());
    }

    @PostMapping("/{chatId}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock group chat",
            description = "Admin-only: lifts a previously applied block on a group chat")
    public void unblockGroupChat(@PathVariable Long chatId,
                                 @Valid @RequestBody GroupModerationRequestDto request) {

        chatService.unblockGroupChat(chatId, request.reason());
    }
}
