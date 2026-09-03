package org.example.entity.type;

import java.util.Locale;

public enum Language {
    EN(Locale.ENGLISH),
    RU(new Locale("ru")),
    UK(new Locale("uk"));

    private final Locale locale;

    Language(Locale locale) {
        this.locale = locale;
    }

    public Locale toLocale() {
        return locale;
    }
}
