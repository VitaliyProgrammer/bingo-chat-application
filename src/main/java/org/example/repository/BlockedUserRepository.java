package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    Optional<BlockedUser> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    @Query("""
            SELECT b FROM BlockedUser b
            JOIN FETCH b.blocked
            WHERE b.blocker.id = :blockerId
            ORDER BY b.createdAt DESC
            """)
    List<BlockedUser> findAllByBlockerId(@Param("blockerId") Long blockerId);

    @Query("""
            SELECT COUNT(b) > 0 FROM BlockedUser b
            WHERE (b.blocker.id = :userId1 AND b.blocked.id = :userId2)
               OR (b.blocker.id = :userId2 AND b.blocked.id = :userId1)
            """)
    boolean existsBlockBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
