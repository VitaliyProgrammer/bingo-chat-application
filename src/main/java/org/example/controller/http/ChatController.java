package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.example.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management for API`s")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Create private chat", description = "Create of get existing private chat")
    public ChatResponseDto createPrivateChat(@RequestParam Long userId) {

        return chatService.createPrivateChat(userId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my chats", description = "Get all chats for the current user")
    public List<ChatResponseDto> getMyChat() {

        return chatService.getMyChats();
    }

    @GetMapping("list")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my chatList", description = "Get all ")
    public List<ChatListItemResponseDto> getChatList() {

        return chatService.getMyChatList();
    }

}
