package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.id = :senderUserId")
    List<Chat> findAllChatsByUserId(Long userId);

    @Query("""
            SELECT c FROM Chat c
            JOIN c.participants p1
            JOIN c.participants p2
            WHERE c.chatType = 'PRIVATE'
            AND p1.id = :user1Id AND p2.id = :user2Id AND p1.id <> p2.id""")
    Optional<Chat> findPrivateChatBetweenUsers(Long user1Id, Long user2Id);
}
