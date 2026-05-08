package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.FeedbackRequestDto;
import org.example.dto.response.FeedbackResponseDto;
import org.example.entity.Feedback;
import org.example.entity.User;
import org.example.mapper.FeedbackMapper;
import org.example.repository.FeedbackRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.FeedbackService;
import org.springframework.stereotype.Service;

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
}
