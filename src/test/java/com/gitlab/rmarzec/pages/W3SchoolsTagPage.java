package com.gitlab.rmarzec.pages;

import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;
import com.gitlab.rmarzec.support.ConsentHandler;
import com.gitlab.rmarzec.support.ConsentLocators;

/**
 * W3Schools reference page of an HTML tag, for example
 * {@code https://www.w3schools.com/tags/tag_select.asp}.
 */
public class W3SchoolsTagPage extends BasePage {

    private static final By[] TRY_IT_YOURSELF_BUTTONS = {
            By.cssSelector("a.tryitbtn"),
            By.cssSelector("a.w3-btn[href*='tryit.asp']"),
            By.cssSelector("a[href*='tryit.asp']"),
            By.xpath("//a[contains(., 'Try it Yourself')]")
    };

    private final ConsentHandler consentHandler;

    public W3SchoolsTagPage(WebDriver driver) {
        super(driver);
        this.consentHandler = new ConsentHandler(driver);
    }

    public W3SchoolsTagPage open(String url) {
        driver.get(url);
        waitUntilDocumentIsReady();
        return this;
    }

    public W3SchoolsTagPage acceptCookiesIfPresent() {
        consentHandler.acceptIfPresent("W3Schools", ConsentLocators.W3SCHOOLS);
        return this;
    }

    /**
     * Clicks the first "Try it Yourself" button. The link opens a new tab, so the driver is
     * switched to it when one appears; when the editor replaces the current tab instead, the
     * existing window keeps the focus.
     *
     * @return the editor page, already focused on the right window
     */
    public W3SchoolsTryItPage clickFirstTryItYourself() {
        WebElement tryItButton = findFirstDisplayed(TestConfig.explicitWait(), TRY_IT_YOURSELF_BUTTONS);
        if (tryItButton == null) {
            throw new IllegalStateException("No 'Try it Yourself' button was found on " + currentUrl());
        }
        ConsoleLog.info("Clicking", readText(tryItButton) + " -> " + readAttribute(tryItButton, "href"));

        Set<String> windowsBeforeClick = windowHandles();
        click(tryItButton);

        if (switchToNewWindow(windowsBeforeClick)) {
            ConsoleLog.info("Switched to the editor window", currentUrl());
        } else {
            ConsoleLog.info("The editor opened in the current window", currentUrl());
        }
        return new W3SchoolsTryItPage(driver);
    }
}
