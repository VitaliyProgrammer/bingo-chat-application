package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.MessageReminderRequestDto;
import org.example.dto.response.ChatResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.service.ChatService;
import org.example.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Tag(name = "Saved Messages API", description = "API for personal self-chat functionality")
public class NoteController {

    private final ChatService chatService;
    private final MessageService messageService;

    @PostMapping("/self")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create SELF chat",
            description = "Create personal chat for authenticated user")
    public ChatResponseDto createSelfChat() {

        return chatService.createSelfChat();
    }

    @PostMapping("/messages/{id}/pin")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Pin message in the SELF chat",
            description = "Pins message inside personal chat")
    public MessageResponseDto pinMessage(@PathVariable Long id) {

        return messageService.pinMessage(id);
    }

    @PostMapping("/{id}/reminder")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create reminder for the message",
            description = "Create reminder for selected message")
    public MessageResponseDto reminderMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageReminderRequestDto request) {

        return messageService.setReminder(id, request);
    }
}
