package org.example.configuration.internationalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.entity.type.Language;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class InternationalizationConfigurationTest {

    private final MessageSource messageSource = new InternationalizationConfiguration().messageSource();

    @Test
    void getMessage_ukrainianLocale_resolvesFromMessagesUkFile() {
        String message = messageSource.getMessage("user.online", null, Language.UK.toLocale());

        assertThat(message).isEqualTo("Онлайн");
    }

    @Test
    void getMessage_russianLocale_resolvesFromMessagesRuFile() {
        String message = messageSource.getMessage("user.online", null, Language.RU.toLocale());

        assertThat(message).isEqualTo("В сети");
    }

    @Test
    void getMessage_englishLocale_resolvesFromDefaultBundle() {
        String message = messageSource.getMessage("user.online", null, Language.EN.toLocale());

        assertThat(message).isEqualTo("Online");
    }

    @Test
    void getMessage_keyMissingFromLocaleSpecificBundle_fallsBackToDefaultBundle() {
        String message = messageSource.getMessage("email.notBlank", null, Language.UK.toLocale());

        assertThat(message).isEqualTo("Email must not be empty!");
    }
}
