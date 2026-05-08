package org.example.repository;

import java.util.List;
import org.example.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
