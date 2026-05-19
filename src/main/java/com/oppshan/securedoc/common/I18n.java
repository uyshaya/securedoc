package com.oppshan.securedoc.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Lightweight wrapper around {@link ResourceBundle} for the {@code messages} baseline. JSF pages still look messages up
 * via {@code <f:loadBundle basename="messages" var="msg"/>}; this service is the Java-side equivalent for beans,
 * services, and mail templates.
 *
 * <p>English (the default locale) is the only locale currently shipped.
 * Adding a new locale means dropping {@code messages_<tag>.properties} next to {@code messages.properties} on the
 * classpath; no code change required.
 */
@Named
@ApplicationScoped
public class I18n {

    private static final String BUNDLE_BASE_NAME = "messages";

    public String get(String key) {
        return bundleFor(Locale.getDefault()).getString(key);
    }

    public String get(String key, Object argument) {
        return get(key, new Object[] {argument});
    }

    public String get(String key, Object... arguments) {
        final var template = bundleFor(Locale.getDefault()).getString(key);
        return MessageFormat.format(template, arguments);
    }

    public String get(Locale locale, String key) {
        return bundleFor(locale).getString(key);
    }

    public String get(Locale locale, String key, Object... arguments) {
        final var template = bundleFor(locale).getString(key);
        return MessageFormat.format(template, arguments);
    }

    private static ResourceBundle bundleFor(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
    }
}
