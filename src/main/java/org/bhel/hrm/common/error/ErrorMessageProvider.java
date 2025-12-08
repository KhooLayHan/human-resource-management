package org.bhel.hrm.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ErrorMessageProvider {
    private static final Logger logger = LoggerFactory.getLogger(ErrorMessageProvider.class);

    private final String baseName;
    private final Locale defaultLocale;
    private final ConcurrentHashMap<Locale, ResourceBundle> bundleCache;
    private final ResourceBundle.Control control;

    /**
     * Constructor with custom configuration.
     *
     * @param baseName Base name of resource bundle (e.g., "error-messages")
     * @param defaultLocale Default locale to use
     */
    public ErrorMessageProvider(String baseName, Locale defaultLocale) {
        this.baseName = baseName;
        this.defaultLocale = defaultLocale;
        this.bundleCache = new ConcurrentHashMap<>();
        this.control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);

        logger.info("Initialized ErrorMessageProvider with baseName: {}, defaultLocale: {}",
            baseName, defaultLocale);
    }

    /**
     * Constructor with default configuration
     */
    public ErrorMessageProvider(String baseName) {
        this(baseName, Locale.getDefault());
    }

    /**
     * Default constructor – uses "error-messages" as base name
     */
    public ErrorMessageProvider() {
        this("error-messages");
    }

    /**
     * Gets message for error code using default locale
     */
    public String getMessage(ErrorCode errorCode) {
        return getMessage(errorCode, defaultLocale);
    }

    /**
     * Gets message for error code with specific locale
     */
    public String getMessage(ErrorCode errorCode, Locale locale) {
        String code = errorCode.getCode();

        try {
            ResourceBundle bundle = getBundle(locale);

            if (bundle.containsKey(code))
                return bundle.getString(code);
        } catch (MissingResourceException e) {
            logger.debug("Resource bundle not found for locale: {}", locale);
        }

        // Fallback to error code's default message
        logger.debug("Using default message for code: {}", code);
        return errorCode.getDefaultMessage();
    }

    /**
     * Gets message with parameter substitution
     * <p>
     * Example:
     * Properties: HRM-USER-001=User {0} not found in department {1}
     * Usage: getMessage(USER_NOT_FOUND, "John", "IT")
     * Result: "User John not found in department IT"
     */
    public String getMessage(ErrorCode errorCode, Object... params) {
        return getMessage(errorCode, defaultLocale, params);
    }

    /**
     * Gets localized message with parameter substitution
     */
    public String getMessage(ErrorCode errorCode, Locale locale, Object... params) {
        String pattern = getMessage(errorCode, locale);

        if (params == null || params.length == 0)
            return pattern;

        try {
            return MessageFormat.format(pattern, params);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to format message for code [{}] with params [{}]",
                errorCode.getCode(), params, e);

            return pattern;
        }
    }

    /**
     * Gets ResourceBundle for locale with caching
     */
    private ResourceBundle getBundle(Locale locale) {
        return bundleCache.computeIfAbsent(locale, this::loadBundle);
    }

    /**
     * Loads ResourceBundle for specific locale
     */
    private ResourceBundle loadBundle(Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                baseName,
                locale,
                Thread.currentThread().getContextClassLoader(),
                control
            );

            logger.debug("Loaded resource bundle for locale: {}", locale);
            return bundle;
        } catch (MissingResourceException e) {
            logger.warn("Could not load resource bundle for locale: {}, using fallback.",
                locale, e);

            // Attempts to load default locale as fallback
            if (locale.equals(defaultLocale))
                throw e;

            return ResourceBundle.getBundle(
                baseName,
                locale,
                Thread.currentThread().getContextClassLoader(),
                control
            );
        }
    }

    /**
     * Checks if a message exists for the error code in specified locale
     */
    public boolean hasMessage(ErrorCode errorCode, Locale locale) {
        try {
            ResourceBundle bundle = getBundle(locale);
            return bundle.containsKey(errorCode.getCode());
        } catch (MissingResourceException e) {
            return false;
        }
    }

    /**
     * Checks if message exists using default locale
     */
    public boolean hasMessage(ErrorCode errorCode) {
        return hasMessage(errorCode, defaultLocale);
    }

    /**
     * Clears the bundle cache - useful for hot-reloading in development
    */
    public void clearCache() {
        bundleCache.clear();
        ResourceBundle.clearCache();

        logger.info("Cleared cache from ResourceBundle.");
    }

    /**
     * Get all available locales for this resource bundle.
     */
    public Locale[] getAvailableLocales() {
        return Locale.getAvailableLocales();
    }

    public static ErrorMessageProviderBuilder builder() {
        return new ErrorMessageProviderBuilder();
    }

    public static class ErrorMessageProviderBuilder {
        private String baseName = "error-messages";
        private Locale defaultLocale = Locale.getDefault();

        public ErrorMessageProviderBuilder baseName(String baseName) {
            this.baseName = baseName;
            return this;
        }

        public ErrorMessageProviderBuilder defaultLocale(Locale locale) {
            this.defaultLocale = locale;
            return this;
        }

        public ErrorMessageProviderBuilder defaultLocale(String languageTag) {
            this.defaultLocale = Locale.forLanguageTag(languageTag);
            return this;
        }

        public ErrorMessageProvider build() {
            return new ErrorMessageProvider(baseName, defaultLocale);
        }
    }
}

