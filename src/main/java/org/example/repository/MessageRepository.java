package org.example.repository;

import java.util.List;
import org.example.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByChatId(Long chatId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Message m
            SET m.status = 'READ'
            WHERE m.chat.id = :chatId
            AND m.sender.id != :userId
            AND m.status <> 'READ'
            """)
    int markAllMessagesInChatAsRead(Long chatId, Long userId);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.sequence ASC")
    Page<Message> findAllByChatIdOrdered(Long chatId, Pageable pageable);

    List<Message> findAllByChatIdAndIsPinnedTrue(Long chatId);

    @Query(value = "SELECT id FROM messages WHERE id = :messageId FOR UPDATE", nativeQuery = true)
    Long lockMessageForUpdate(@Param("messageId") Long messageId);
}
