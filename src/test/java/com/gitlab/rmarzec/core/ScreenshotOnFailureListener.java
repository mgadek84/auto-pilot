package com.gitlab.rmarzec.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Saves a PNG screenshot and the page URL whenever a browser test fails, which makes
 * failures on a CI machine diagnosable without re-running them locally.
 */
public class ScreenshotOnFailureListener implements ITestListener {

    private static final Path SCREENSHOT_DIRECTORY = Paths.get("target", "screenshots");
    private static final ThreadLocal<WebDriver> ACTIVE_DRIVER = new ThreadLocal<WebDriver>();

    static void registerDriver(WebDriver driver) {
        ACTIVE_DRIVER.set(driver);
    }

    static void unregisterDriver() {
        ACTIVE_DRIVER.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = ACTIVE_DRIVER.get();
        if (!(driver instanceof TakesScreenshot)) {
            return;
        }
        try {
            ConsoleLog.info("Failure URL", driver.getCurrentUrl());
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.createDirectories(SCREENSHOT_DIRECTORY);
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File target = SCREENSHOT_DIRECTORY
                    .resolve(result.getTestClass().getRealClass().getSimpleName()
                            + "-" + result.getMethod().getMethodName() + "-" + timestamp + ".png")
                    .toFile();
            Files.write(target.toPath(), screenshot);
            ConsoleLog.info("Screenshot", target.getAbsolutePath());
        } catch (IOException | RuntimeException screenshotFailed) {
            ConsoleLog.info("Screenshot could not be captured", String.valueOf(screenshotFailed.getMessage()));
        }
    }
}
