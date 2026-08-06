package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.EditMessageRequestDto;
import org.example.dto.request.ReactionRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Message", description = "Operations related to chat messages")
public class MessageController {

    private final MessageService messageService;

/*    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send message",
            description = "Send message to a chat")
    public MessageResponseDto sendMessage(@Valid @RequestBody MessageRequestDto request) {

        return messageService.sendMessage(request);
    }*/

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Edit message",
            description = "Edit your own message")
    public MessageResponseDto editMessage(@PathVariable Long id,
                                          @Valid @RequestBody EditMessageRequestDto request) {

        return messageService.editMessage(id, request.content());
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

    @PostMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pin message",
            description = "Pin a message in the chat")
    public MessageResponseDto pinMessage(@PathVariable Long id) {

        return messageService.pinMessage(id);
    }

    @PostMapping("/{id}/reactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add or replace reaction",
            description = "Sets the caller's reaction on a message, replacing any previous "
                    + "reaction from the same user")
    public MessageResponseDto addReaction(@PathVariable Long id,
                                          @Valid @RequestBody ReactionRequestDto request) {

        return messageService.addReaction(id, request);
    }

    @DeleteMapping("/{id}/reactions")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Remove reaction",
            description = "Removes the caller's own reaction from a message")
    public MessageResponseDto removeReaction(@PathVariable Long id) {

        return messageService.removeReaction(id);
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
