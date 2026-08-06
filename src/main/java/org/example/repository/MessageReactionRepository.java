package org.example.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessageId(Long messageId);

    List<MessageReaction> findByMessageIdIn(Collection<Long> messageIds);

    Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

    void deleteByMessageIdAndUserId(Long messageId, Long userId);
}
