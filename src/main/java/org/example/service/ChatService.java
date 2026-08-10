package org.example.service;

import java.util.List;
import org.example.dto.request.AddParticipantsRequestDto;
import org.example.dto.request.GroupChatRequestDto;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    ChatResponseDto createPrivateChat(Long userId);

    List<ChatListItemResponseDto> getMyChatList();

    ChatResponseDto createSelfChat();

    ChatResponseDto getSelfChat();

    ChatResponseDto createGroupChat(GroupChatRequestDto request);

    ChatResponseDto addParticipants(Long chatId, AddParticipantsRequestDto request);

    ChatResponseDto removeParticipant(Long chatId, Long userId);

    ChatResponseDto transferOwnership(Long chatId, Long newOwnerId);

    void leaveGroupChat(Long chatId);

    String updateGroupAvatar(Long chatId, MultipartFile file);

    void deleteGroupAvatar(Long chatId);

    void blockGroupChat(Long chatId, String reason);

    void unblockGroupChat(Long chatId, String reason);
}
