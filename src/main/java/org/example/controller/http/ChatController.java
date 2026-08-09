package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AddParticipantsRequestDto;
import org.example.dto.request.GroupChatRequestDto;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.example.service.ChatService;
import org.example.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create group chat",
            description = "Creates a new group chat with the given name and participants")
    public ChatResponseDto createGroupChat(@Valid @RequestBody GroupChatRequestDto request) {

        return chatService.createGroupChat(request);
    }

    @PostMapping("/{chatId}/participants")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Add participants to group",
            description = "Owner-only: adds new participants to an existing group chat")
    public ChatResponseDto addParticipants(@PathVariable Long chatId,
                                           @Valid @RequestBody AddParticipantsRequestDto request) {

        return chatService.addParticipants(chatId, request);
    }

    @DeleteMapping("/{chatId}/participants/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Remove participant from group",
            description = "Owner-only: removes a participant from a group chat")
    public ChatResponseDto removeParticipant(@PathVariable Long chatId, @PathVariable Long userId) {

        return chatService.removeParticipant(chatId, userId);
    }

    @PostMapping(value = "/{chatId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Upload/Update group avatar",
            description = "Owner-only: uploads a new avatar image for the group chat")
    public String updateGroupAvatar(@PathVariable Long chatId,
                                    @RequestParam("file") MultipartFile file) {

        return chatService.updateGroupAvatar(chatId, file);
    }

    @DeleteMapping("/{chatId}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete group avatar",
            description = "Owner-only: removes the avatar image of the group chat")
    public void deleteGroupAvatar(@PathVariable Long chatId) {

        chatService.deleteGroupAvatar(chatId);
    }

    @PostMapping("/{chatId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leave group chat",
            description = "Leaves a group chat; the owner can only leave once no one else remains")
    public void leaveGroupChat(@PathVariable Long chatId) {

        chatService.leaveGroupChat(chatId);
    }

    @PostMapping("/{chatId}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Get my chatList", description = "Get all ")
    public void openChat(@PathVariable Long chatId) {

        messageService.openChat(chatId);
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
