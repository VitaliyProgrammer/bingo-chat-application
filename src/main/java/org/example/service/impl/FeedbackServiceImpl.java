package org.example.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.FeedbackRequestDto;
import org.example.dto.request.FeedbackUpdateRequestDto;
import org.example.dto.response.AdminFeedbackStatsResponseDto;
import org.example.dto.response.FeedbackPageResponseDto;
import org.example.dto.response.FeedbackResponseDto;
import org.example.entity.Feedback;
import org.example.entity.User;
import org.example.entity.status.FeedbackStatus;
import org.example.exception.FeedbackNotFoundException;
import org.example.mapper.FeedbackMapper;
import org.example.repository.FeedbackRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final CurrentUserProvider currentUserProvider;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    @Override
    public FeedbackResponseDto createFeedback(FeedbackRequestDto request) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        log.debug("Creating feedback: userId={}, type={}", currentUser.getId(), request.type());

        Feedback feedback = new Feedback();

        feedback.setUser(currentUser);

        feedback.setEmailSnapshot(request.email());

        feedback.setType(request.type());

        feedback.setMessage(request.message());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        log.info("Feedback created: feedback={}, userId={}, type={}",
                savedFeedback.getId(), currentUser.getId(), savedFeedback.getId());

        return feedbackMapper.toDto(savedFeedback);
    }

    @Override
    @Transactional
    public List<FeedbackResponseDto> getMyFeedback() {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        List<Feedback> feedback =
                feedbackRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        feedback.stream()
                .filter(item -> item.getAdminReply() != null && !item.isReplySeen())
                .forEach(item -> item.setReplySeen(true));

        return feedback.stream()
                .map(feedbackMapper::toDto)
                .toList();
    }

    @Override
    public FeedbackPageResponseDto getAllFeedback(int page, int size) {

        Page<Feedback> feedbackPage = feedbackRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return feedbackMapper.toPageDto(feedbackPage);
    }

    @Override
    @Transactional
    public FeedbackResponseDto updateFeedback(Long feedbackId, FeedbackUpdateRequestDto request) {

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException("Feedback not found!"));

        if (request.status() != null) {
            feedback.setStatus(request.status());
        }

        if (request.adminReply() != null) {
            feedback.setAdminReply(request.adminReply());
            feedback.setRespondedAt(LocalDateTime.now());
        }

        log.info("Feedback updated: feedbackId={}, status={}", feedbackId, feedback.getStatus());

        return feedbackMapper.toDto(feedback);
    }

    @Override
    public AdminFeedbackStatsResponseDto getFeedbackStats() {

        int pendingFeedbackCount = (int) feedbackRepository.countByStatus(FeedbackStatus.NEW);

        return new AdminFeedbackStatsResponseDto(pendingFeedbackCount);
    }
}
