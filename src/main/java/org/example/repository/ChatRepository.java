package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.entity.Chat;
import org.example.entity.type.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("""
            SELECT c FROM Chat c
            JOIN c.participants p
            WHERE p.id = :userId
            ORDER BY c.lastActivityTime DESC
            """)
    List<Chat> findAllChatsByUserId(Long userId);

    @Query("""
            SELECT c FROM Chat c
            JOIN c.participants p1
            JOIN c.participants p2
            WHERE c.chatType = 'PRIVATE'
            AND p1.id = :user1Id AND p2.id = :user2Id AND p1.id <> p2.id""")
    Optional<Chat> findPrivateChatBetweenUsers(Long user1Id, Long user2Id);

    /**
     * Atomically generates next message sequence for chat using database function.
     * This approach guarantees uniqueness even under high concurrent load.
     * MUST be called within an active transaction.
     *
     * @param chatId the chat ID
     * @return next sequence number
     */
    @Query(value = "SELECT get_next_message_sequence(:chatId)", nativeQuery = true)
    Long getNextMessageSequence(@Param("chatId") Long chatId);

    /**
     * Lock chat row for update to prevent concurrent sequence generation.
     * Use this before calling getNextMessageSequence().
     *
     * @param chatId the chat ID
     */
    @Query(value = "SELECT id FROM chats WHERE id = :chatId FOR UPDATE", nativeQuery = true)
    Long lockChatForUpdate(@Param("chatId") Long chatId);

    boolean existsByIdAndParticipants_Id(Long chatId, Long userId);

    boolean existsByOwnerIdAndChatType(Long ownerId, ChatType chatType);

    Optional<Chat> findByOwnerIdAndChatType(Long ownerId, ChatType chatType);
}
