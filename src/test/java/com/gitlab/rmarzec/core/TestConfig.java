package com.gitlab.rmarzec.core;

import java.time.Duration;

/**
 * Single place where all runtime switches of the framework are resolved.
 *
 * <p>Every value can be overridden from the command line, for example:
 * {@code mvn test -Dbrowser=firefox -Dheadless=false -DexplicitWaitSeconds=40}.
 */
public final class TestConfig {

    private static final String BROWSER_PROPERTY = "browser";
    private static final String HEADLESS_PROPERTY = "headless";
    private static final String EXPLICIT_WAIT_PROPERTY = "explicitWaitSeconds";
    private static final String PAGE_LOAD_TIMEOUT_PROPERTY = "pageLoadTimeoutSeconds";
    private static final String BROWSER_LANGUAGE_PROPERTY = "browserLanguage";

    private static final String DEFAULT_BROWSER = "chrome";
    private static final long DEFAULT_EXPLICIT_WAIT_SECONDS = 25L;
    private static final long DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS = 60L;
    private static final String DEFAULT_BROWSER_LANGUAGE = "pl-PL";

    private TestConfig() {
    }

    public static Browser browser() {
        return Browser.fromName(property(BROWSER_PROPERTY, DEFAULT_BROWSER));
    }

    public static boolean headless() {
        return Boolean.parseBoolean(property(HEADLESS_PROPERTY, Boolean.TRUE.toString()));
    }

    /** Timeout used by every {@code WebDriverWait} created in the framework. */
    public static Duration explicitWait() {
        return Duration.ofSeconds(longProperty(EXPLICIT_WAIT_PROPERTY, DEFAULT_EXPLICIT_WAIT_SECONDS));
    }

    /** Shorter timeout for optional elements such as cookie banners. */
    public static Duration shortWait() {
        long seconds = Math.max(3L, explicitWait().getSeconds() / 3L);
        return Duration.ofSeconds(seconds);
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(longProperty(PAGE_LOAD_TIMEOUT_PROPERTY, DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS));
    }

    /**
     * Browser UI language. The tasks are written against the Polish UI (for example the
     * "Szczęśliwy traf" button), so a Polish locale is requested by default.
     */
    public static String browserLanguage() {
        return property(BROWSER_LANGUAGE_PROPERTY, DEFAULT_BROWSER_LANGUAGE);
    }

    private static String property(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        String trimmedValue = value.trim();
        // An unresolved Maven placeholder ("${headless}") means the property was not set.
        boolean unresolvedPlaceholder = ("${" + name + "}").equals(trimmedValue);
        return trimmedValue.isEmpty() || unresolvedPlaceholder ? defaultValue : trimmedValue;
    }

    private static long longProperty(String name, long defaultValue) {
        try {
            return Long.parseLong(property(name, Long.toString(defaultValue)));
        } catch (NumberFormatException notANumber) {
            return defaultValue;
        }
    }

    public enum Browser {
        CHROME,
        FIREFOX;

        static Browser fromName(String name) {
            for (Browser candidate : values()) {
                if (candidate.name().equalsIgnoreCase(name)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unsupported browser: '" + name + "'. Supported values: chrome, firefox.");
        }
    }
}
