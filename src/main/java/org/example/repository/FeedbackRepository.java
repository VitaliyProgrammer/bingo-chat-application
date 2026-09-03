package org.example.repository;

import java.util.List;
import org.example.entity.Feedback;
import org.example.entity.status.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(FeedbackStatus status);

    long countByUserIdAndAdminReplyIsNotNullAndReplySeenFalse(Long userId);
}
