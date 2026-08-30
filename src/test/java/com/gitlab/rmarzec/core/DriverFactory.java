package com.gitlab.rmarzec.core;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Creates and configures the {@link WebDriver} instance used by the tests.
 *
 * <p>The driver binary is resolved by Selenium Manager (shipped with Selenium 4), so no
 * driver has to be committed to the repository. When a driver is already available locally
 * it can be pinned with {@code -Dwebdriver.chrome.driver=/path/to/chromedriver}.
 */
public final class DriverFactory {

    /**
     * Strong references are required: {@code java.util.logging} keeps only weak references to
     * loggers, so a configured logger that is not referenced can be collected and lose its
     * level.
     */
    private static final Logger CDP_VERSION_FINDER_LOGGER =
            Logger.getLogger("org.openqa.selenium.devtools.CdpVersionFinder");
    private static final Logger CHROMIUM_DRIVER_LOGGER =
            Logger.getLogger("org.openqa.selenium.chromium.ChromiumDriver");

    static {
        silenceChromeDevToolsWarnings();
    }

    private DriverFactory() {
    }

    public static WebDriver createDriver() {
        WebDriver driver;
        switch (TestConfig.browser()) {
            case FIREFOX:
                driver = new FirefoxDriver(firefoxOptions());
                break;
            case CHROME:
            default:
                driver = new ChromeDriver(chromeOptions());
                break;
        }
        applyTimeouts(driver);
        return driver;
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(commonChromeArguments());
        options.addArguments("--lang=" + TestConfig.browserLanguage());
        if (TestConfig.headless()) {
            options.addArguments("--headless=new");
        }
        // Keeps the automation banner and password manager popups out of the way.
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        return options;
    }

    private static List<String> commonChromeArguments() {
        return Arrays.asList(
                "--window-size=1920,1080",
                "--start-maximized",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-notifications",
                "--disable-popup-blocking",
                "--disable-search-engine-choice-screen",
                "--remote-allow-origins=*");
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1920", "--height=1080");
        options.addPreference("intl.accept_languages", TestConfig.browserLanguage());
        options.addPreference("dom.webnotifications.enabled", false);
        if (TestConfig.headless()) {
            options.addArguments("-headless");
        }
        return options;
    }

    /**
     * These tests drive the browser through the WebDriver protocol only. Selenium still logs
     * a warning when it cannot match a Chrome DevTools Protocol version to the installed
     * Chrome, which would otherwise bury the output the tasks are supposed to print.
     */
    private static void silenceChromeDevToolsWarnings() {
        CDP_VERSION_FINDER_LOGGER.setLevel(Level.SEVERE);
        CHROMIUM_DRIVER_LOGGER.setLevel(Level.SEVERE);
    }

    private static void applyTimeouts(WebDriver driver) {
        driver.manage().timeouts().pageLoadTimeout(TestConfig.pageLoadTimeout());
        // No implicit wait on purpose: mixing implicit and explicit waits makes timings unpredictable.
    }
}
