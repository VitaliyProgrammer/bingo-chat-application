package org.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.example.entity.User;
import org.example.entity.type.Language;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserProvider currentUserProvider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentLocale_ukrainianPreference_returnsUkrainianLocale() {
        authenticateAs(userWithLanguage(Language.UK));

        Locale locale = currentUserProvider.getCurrentLocale();

        assertThat(locale).isEqualTo(new Locale("uk"));
    }

    @Test
    void getCurrentLocale_russianPreference_returnsRussianLocale() {
        authenticateAs(userWithLanguage(Language.RU));

        Locale locale = currentUserProvider.getCurrentLocale();

        assertThat(locale).isEqualTo(new Locale("ru"));
    }

    @Test
    void getCurrentLocale_defaultPreference_returnsEnglishLocale() {
        authenticateAs(userWithLanguage(Language.EN));

        Locale locale = currentUserProvider.getCurrentLocale();

        assertThat(locale).isEqualTo(Locale.ENGLISH);
    }

    private User userWithLanguage(Language language) {
        User user = new User();
        user.setPreferredLanguage(language);
        return user;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserSecurity(user), null, List.of()));
    }
}
