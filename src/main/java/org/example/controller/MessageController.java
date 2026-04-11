package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Message", description = "Operations related to chat messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send message",
            description = "Send message to a chat")
    public MessageResponseDto sendMessage(@RequestBody MessageRequestDto request) {

        return messageService.sendMessage(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Edit message",
            description = "Edit your own message")
    public MessageResponseDto editMessage(@PathVariable Long id, @RequestParam String newContent) {

        return messageService.editMessage(id, newContent);
    }

    @PostMapping("/{id}/delivered")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Mark as delivered",
            description = "Mark message as delivered")
    public MessageResponseDto markAsDelivered(@PathVariable Long id) {

        return messageService.markAsDelivered(id);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Mark as read",
            description = "Mark message as read")
    public MessageResponseDto markAsRead(@PathVariable Long id) {

        return messageService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Delete message",
            description = "Delete your message")
    public void deleteMessage(@PathVariable Long id) {

        messageService.deleteMessage(id);
    }

    @GetMapping("/chat/{chatId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get history messages of the chat",
            description = "Retrieve paginated messages for the specific chat(infinitive scrolling)")
    public MessagePageResponseDto getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return messageService.getChatMessages(chatId, page, size);
    }
}
