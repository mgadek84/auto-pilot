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
            driver.quit();
            driver = null;
        }
    }
}
