package com.gitlab.rmarzec.support;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;

/**
 * Dismisses cookie / consent banners.
 *
 * <p>Consent banners are optional by nature - they depend on the region, on stored cookies
 * and on the consent platform in use - so nothing here fails the test when no banner shows
 * up. Banners rendered inside an iframe (a common pattern for consent platforms) are handled
 * by scanning the frames of the page.
 */
public class ConsentHandler extends BasePage {

    private static final By IFRAME = By.tagName("iframe");
    private static final int MAX_SCANNED_FRAMES = 10;

    public ConsentHandler(WebDriver driver) {
        super(driver);
    }

    /**
     * Clicks the first displayed accept button, looking both in the main document and inside
     * iframes.
     *
     * @param bannerName human readable name used in the console output
     * @param acceptButtons candidate locators of the "accept" button, most specific first
     * @return {@code true} when a banner was accepted
     */
    public boolean acceptIfPresent(String bannerName, By... acceptButtons) {
        if (clickAcceptInCurrentContext(bannerName, acceptButtons)) {
            return true;
        }
        return clickAcceptInsideFrames(bannerName, acceptButtons);
    }

    private boolean clickAcceptInCurrentContext(String bannerName, By... acceptButtons) {
        WebElement acceptButton = findFirstDisplayed(TestConfig.shortWait(), acceptButtons);
        if (acceptButton == null) {
            return false;
        }
        String label = readText(acceptButton);
        click(acceptButton);
        ConsoleLog.info("Accepted " + bannerName + " cookie banner", label.isEmpty() ? "<no label>" : label);
        return true;
    }

    private boolean clickAcceptInsideFrames(String bannerName, By... acceptButtons) {
        List<WebElement> frames = safeFindElements(IFRAME);
        int framesToScan = Math.min(frames.size(), MAX_SCANNED_FRAMES);
        for (int index = 0; index < framesToScan; index++) {
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame(index);
                if (findFirstDisplayedNow(acceptButtons) != null
                        && clickAcceptInCurrentContext(bannerName, acceptButtons)) {
                    driver.switchTo().defaultContent();
                    return true;
                }
            } catch (NoSuchFrameException frameDisappeared) {
                // The frame was removed while scanning - continue with the next one.
            } finally {
                driver.switchTo().defaultContent();
            }
        }
        ConsoleLog.info("No " + bannerName + " cookie banner shown");
        return false;
    }
}
