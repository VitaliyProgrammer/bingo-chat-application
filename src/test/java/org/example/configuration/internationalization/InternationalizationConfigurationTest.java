package org.example.configuration.internationalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
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
    void getMessage_ukrainianLocale_translatesValidationMessage() {
        String message = messageSource.getMessage("email.notBlank", null, Language.UK.toLocale());

        assertThat(message).isEqualTo("Email не може бути порожнім!");
    }

    @Test
    void getMessage_russianLocale_translatesValidationMessage() {
        String message = messageSource.getMessage("email.notBlank", null, Language.RU.toLocale());

        assertThat(message).isEqualTo("Email не может быть пустым!");
    }

    @Test
    void allLocaleBundles_haveExactlyTheSameKeysAsTheDefaultBundle() throws IOException {
        Set<String> defaultKeys = loadKeys("messages.properties");
        Set<String> ukKeys = loadKeys("messages_uk.properties");
        Set<String> ruKeys = loadKeys("messages_ru.properties");

        assertThat(ukKeys).containsExactlyInAnyOrderElementsOf(defaultKeys);
        assertThat(ruKeys).containsExactlyInAnyOrderElementsOf(defaultKeys);
    }

    private Set<String> loadKeys(String resourceName) throws IOException {
        Properties properties = new Properties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        return properties.stringPropertyNames();
    }
}
