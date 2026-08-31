package com.gitlab.rmarzec.support;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
    private static final int MAX_SCANNED_FRAMES = 25;

    private static final By CONSENT_OVERLAY = By.cssSelector(
            ".fc-consent-root, .fc-dialog-overlay, #cmpbox, #snigel-cmp-framework, #sp_message_container");

    private static final String[] CONSENT_FRAME_HINTS = {
            "consent", "fundingchoices", "sp_message", "privacymanager", "onetrust", "cmp"
    };

    private static final String CLICK_ACCEPT_BY_LABEL_SCRIPT =
            "var labels = ['Potwierdź', 'Zaakceptuj wszystko', 'Zaakceptuj', 'Zgadzam się',"
                    + " 'Accept all', 'Accept All', 'I accept', 'Accept & continue', 'I agree', 'Agree'];"
                    + "var nodes = document.querySelectorAll('button, [role=\"button\"], .fc-cta-consent, .fc-button');"
                    + "for (var i = 0; i < nodes.length; i++) {"
                    + "  var text = ((nodes[i].innerText || nodes[i].textContent || '') + ' '"
                    + "    + (nodes[i].getAttribute('aria-label') || '')).replace(/\\s+/g, ' ').trim();"
                    + "  for (var j = 0; j < labels.length; j++) {"
                    + "    if (text.indexOf(labels[j]) !== -1) { nodes[i].click(); return labels[j]; }"
                    + "  }"
                    + "}"
                    + "return null;";

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
        boolean accepted = clickAcceptInCurrentContext(bannerName, acceptButtons)
                || clickAcceptByLabelWithScript(bannerName)
                || clickAcceptInsideFrames(bannerName, acceptButtons);
        waitForOverlayToDisappear();
        return accepted;
    }

    /**
     * Same as {@link #acceptIfPresent} but without waiting: used on pages where sitting still
     * for a cookie banner is more dangerous than missing it (the W3Schools editor).
     */
    public boolean acceptIfPresentNow(String bannerName, By... acceptButtons) {
        WebElement acceptButton = findFirstDisplayedNow(acceptButtons);
        if (acceptButton != null) {
            try {
                String label = readText(acceptButton);
                click(acceptButton);
                ConsoleLog.info("Accepted " + bannerName + " cookie banner",
                        label.isEmpty() ? "<no label>" : label);
                waitForOverlayToDisappear();
                return true;
            } catch (WebDriverException clickFailed) {
                return clickAcceptByLabelWithScript(bannerName);
            }
        }
        return clickAcceptByLabelWithScript(bannerName);
    }

    private boolean clickAcceptInCurrentContext(String bannerName, By... acceptButtons) {
        WebElement acceptButton = findFirstDisplayed(TestConfig.shortWait(), acceptButtons);
        if (acceptButton == null) {
            return false;
        }
        String label = readText(acceptButton);
        try {
            click(acceptButton);
        } catch (WebDriverException clickFailed) {
            return clickAcceptByLabelWithScript(bannerName);
        }
        ConsoleLog.info("Accepted " + bannerName + " cookie banner", label.isEmpty() ? "<no label>" : label);
        return true;
    }

    /**
     * Funding Choices and similar CMPs often render Polish labels ("Potwierdź") that are easy
     * to miss with a fixed locator set, or they sit in a layer Selenium does not treat as
     * displayed. A scripted click by visible label covers those cases without a sleep.
     */
    private boolean clickAcceptByLabelWithScript(String bannerName) {
        try {
            Object clickedLabel = ((JavascriptExecutor) driver).executeScript(CLICK_ACCEPT_BY_LABEL_SCRIPT);
            if (clickedLabel instanceof String && !((String) clickedLabel).isEmpty()) {
                ConsoleLog.info("Accepted " + bannerName + " cookie banner", (String) clickedLabel);
                return true;
            }
        } catch (WebDriverException scriptFailed) {
            // The page may still be loading the CMP - the iframe scan below is the next try.
        }
        return false;
    }

    private boolean clickAcceptInsideFrames(String bannerName, By... acceptButtons) {
        List<WebElement> frames = safeFindElements(IFRAME);
        int framesToScan = Math.min(frames.size(), MAX_SCANNED_FRAMES);
        for (int index = 0; index < framesToScan; index++) {
            if (!isConsentFrame(frames, index) && index >= 10) {
                continue;
            }
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame(index);
                if (findFirstDisplayedNow(acceptButtons) != null
                        && clickAcceptInCurrentContext(bannerName, acceptButtons)) {
                    driver.switchTo().defaultContent();
                    return true;
                }
                if (clickAcceptByLabelWithScript(bannerName)) {
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

    private boolean isConsentFrame(List<WebElement> frames, int index) {
        if (index < 0 || index >= frames.size()) {
            return false;
        }
        try {
            WebElement frame = frames.get(index);
            String identity = (readAttribute(frame, "src") + " "
                    + readAttribute(frame, "id") + " "
                    + readAttribute(frame, "name") + " "
                    + readAttribute(frame, "title")).toLowerCase();
            for (String hint : CONSENT_FRAME_HINTS) {
                if (identity.contains(hint)) {
                    return true;
                }
            }
        } catch (WebDriverException frameGone) {
            return false;
        }
        return false;
    }

    private void waitForOverlayToDisappear() {
        waitQuietly(TestConfig.shortWait(), ExpectedConditions.invisibilityOfElementLocated(CONSENT_OVERLAY));
    }
}
