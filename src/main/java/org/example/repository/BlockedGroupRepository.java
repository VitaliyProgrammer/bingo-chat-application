package org.example.repository;

import java.util.Optional;
import org.example.entity.BlockedGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedGroupRepository extends JpaRepository<BlockedGroup, Long> {

    Optional<BlockedGroup> findByChat_IdAndUnblockedAtIsNull(Long chatId);

    boolean existsByChat_IdAndUnblockedAtIsNull(Long chatId);
}
