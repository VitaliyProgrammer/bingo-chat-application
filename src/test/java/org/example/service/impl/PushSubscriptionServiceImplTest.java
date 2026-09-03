package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.dto.request.PushSubscriptionRequestDto;
import org.example.entity.PushSubscription;
import org.example.entity.User;
import org.example.repository.PushSubscriptionRepository;
import org.example.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final String ENDPOINT = "https://push.example.com/endpoint";

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private PushSubscriptionServiceImpl pushSubscriptionService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(CURRENT_USER_ID);
    }

    @Test
    void getPublicKey_returnsConfiguredVapidPublicKey() {

        ReflectionTestUtils.setField(pushSubscriptionService, "publicKey", "test-public-key");

        assertThat(pushSubscriptionService.getPublicKey()).isEqualTo("test-public-key");
    }

    @Test
    void subscribe_newEndpoint_savesSubscriptionForCurrentUser() {

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(currentUser);
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());

        pushSubscriptionService.subscribe(subscriptionRequest());

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());

        PushSubscription saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(currentUser);
        assertThat(saved.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(saved.getP256dhKey()).isEqualTo("p256dh-key");
        assertThat(saved.getAuthKey()).isEqualTo("auth-key");
    }

    @Test
    void subscribe_existingEndpoint_refreshesKeysInsteadOfDuplicating() {

        PushSubscription existing = new PushSubscription();
        existing.setId(7L);
        existing.setEndpoint(ENDPOINT);

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(currentUser);
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(existing));

        pushSubscriptionService.subscribe(subscriptionRequest());

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getP256dhKey()).isEqualTo("p256dh-key");
    }

    @Test
    void unsubscribe_deletesSubscriptionByEndpoint() {

        pushSubscriptionService.unsubscribe(ENDPOINT);

        verify(pushSubscriptionRepository).deleteByEndpoint(ENDPOINT);
    }

    private PushSubscriptionRequestDto subscriptionRequest() {
        return new PushSubscriptionRequestDto(
                ENDPOINT,
                new PushSubscriptionRequestDto.Keys("p256dh-key", "auth-key")
        );
    }
}
