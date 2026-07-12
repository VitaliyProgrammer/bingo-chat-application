package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.example.service.ChatService;
import org.example.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management for API`s")
public class ChatController {

    private final ChatService chatService;

    private final MessageService messageService;

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Create private chat", description = "Create of get existing private chat")
    public ChatResponseDto createPrivateChat(@RequestParam Long userId) {

        return chatService.createPrivateChat(userId);
    }

    @PostMapping("/{chatId}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Get my chatList", description = "Get all ")
    public void openChat(@PathVariable Long chatId) {

        messageService.openChat(chatId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my chats", description = "Get all chats for the current user")
    public List<ChatResponseDto> getMyChat() {

        return chatService.getMyChats();
    }

    @GetMapping("/sidebar")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my chatList", description = "Get all ")
    public List<ChatListItemResponseDto> getChatList() {

        return chatService.getMyChatList();
    }

    @GetMapping("/self")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get self chat", description = "Get personal self chat for "
            + "current authenticated user")
    public ChatResponseDto getSelfChat() {

        return chatService.getSelfChat();
    }
}
