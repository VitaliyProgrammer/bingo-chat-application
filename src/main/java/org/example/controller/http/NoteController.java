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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Tag(name = "Personal Node API", description = "API for SELF chat functionality")
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
    public MessageResponseDto pinMessage(@PathVariable Long messageId) {

        return messageService.pinMessage(messageId);
    }

    @PostMapping("/messages/reminder")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create reminder for the message",
            description = "Create reminder for selected message")
    public MessageResponseDto reminderMessage(
            @Valid @RequestBody MessageReminderRequestDto request) {

        return messageService.setReminder(request);
    }
}
