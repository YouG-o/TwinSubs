package com.twinsubs.ui.service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Service managing internationalization (i18n) resource bundle lookups.
 */
public final class I18nService {

    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static I18nService instance;
    private final ResourceBundle bundle;

    private I18nService() {
        this.bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
    }

    public static synchronized I18nService getInstance() {
        if (instance == null) {
            instance = new I18nService();
        }
        return instance;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public String get(String key) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return "!" + key + "!";
    }

    public String get(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}