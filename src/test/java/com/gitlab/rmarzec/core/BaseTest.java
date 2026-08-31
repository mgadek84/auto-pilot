package com.gitlab.rmarzec.core;

import java.lang.reflect.Method;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Driver lifecycle for browser based tests: a fresh browser session per test method, so a
 * failing task never leaves cookies, tabs or consent state behind for the next one.
 */
@Listeners(ScreenshotOnFailureListener.class)
public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void startBrowser(Method testMethod) {
        ConsoleLog.step("Starting " + TestConfig.browser().name().toLowerCase()
                + (TestConfig.headless() ? " (headless)" : "") + " for " + testMethod.getName());
        driver = DriverFactory.createDriver();
        ScreenshotOnFailureListener.registerDriver(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void quitBrowser() {
        ScreenshotOnFailureListener.unregisterDriver();
        if (driver != null) {
            try {
                driver.quit();
            } catch (RuntimeException sessionAlreadyGone) {
                // The browser may already have crashed; there is nothing left to quit.
            }
            driver = null;
        }
    }

    /**
     * Replaces a dead browser session with a new one. Used when a site (notably the W3Schools
     * editor) kills headless Chrome mid-test and the remaining steps can continue on a fresh
     * window.
     */
    protected void restartBrowser() {
        quitBrowser();
        driver = DriverFactory.createDriver();
        ScreenshotOnFailureListener.registerDriver(driver);
    }
}
