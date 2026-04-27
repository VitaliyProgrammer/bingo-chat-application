package org.example.configuration.outbox.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.example.configuration.outbox.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutBoxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.processed = false
            AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= CURRENT_TIMESTAMP)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findBatchForProcessing(Pageable pageable);
}
