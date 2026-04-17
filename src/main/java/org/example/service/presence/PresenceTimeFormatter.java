package org.example.service.presence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PresenceTimeFormatter {

    private final MessageSource messageSource;

    public String formatLastSeen(long lastSeenMilliSeconds, Locale locale) {

        long diff = System.currentTimeMillis() - lastSeenMilliSeconds;

        if (diff < 20000) {
            return message("last.seen.just.now", locale);
        }

        if (diff < 2 * 60 * 60 * 1000L) {
            long minutes = diff / (60 * 1000);
            return message("last.seen.minutes.ago", locale, minutes);
        }

        if (isToday(lastSeenMilliSeconds)) {
            return message("last.seen.today", locale, time(lastSeenMilliSeconds));
        }

        if (isYesterday(lastSeenMilliSeconds)) {
            return message("last.seen.yesterday", locale, time(lastSeenMilliSeconds));
        }

        if (diff < 7 * 24 * 60 * 60 * 1000L) {
            long days = diff / (24 * 60 * 60 * 1000);
            return message("last.seen.days.ago", locale, days);
        }

        return message("last.seen.date", locale, date(lastSeenMilliSeconds));
    }

    private String message(String key, Locale locale, Object... args) {

        return messageSource.getMessage(key, args, locale);
    }

    private boolean isToday(long milliSeconds) {
        return LocalDate.now().equals(
                Instant.ofEpochMilli(milliSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );
    }

    private boolean isYesterday(long milliSeconds) {
        return LocalDate.now().minusDays(1).equals(
                Instant.ofEpochMilli(milliSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );
    }

    private String time(long milliSeconds) {
        return DateTimeFormatter.ofPattern("HH:mm")
                .format(Instant.ofEpochMilli(milliSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                );
    }

    private String date(long milliSeconds) {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .format(Instant.ofEpochMilli(milliSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                );
    }
}
