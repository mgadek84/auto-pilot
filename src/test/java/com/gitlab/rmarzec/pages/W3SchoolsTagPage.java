package com.gitlab.rmarzec.pages;

import java.net.URI;
import java.net.URISyntaxException;

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
     * Opens the first "Try it Yourself" example.
     *
     * <p>The control is a link with {@code target="_blank"}. A real WebDriver click therefore
     * starts a second Chrome renderer, and headless Chrome 152 regularly kills the whole
     * session when that happens. The first button is still the source of the URL - the editor
     * is then opened in the current tab, which is what the rest of the task needs.
     *
     * @return the editor page, already focused on the right window
     */
    public W3SchoolsTryItPage clickFirstTryItYourself() {
        WebElement tryItButton = findFirstDisplayed(TestConfig.explicitWait(), TRY_IT_YOURSELF_BUTTONS);
        if (tryItButton == null) {
            throw new IllegalStateException("No 'Try it Yourself' button was found on " + currentUrl());
        }
        String editorUrl = absoluteHref(readAttribute(tryItButton, "href"));
        if (editorUrl.isEmpty()) {
            throw new IllegalStateException("The 'Try it Yourself' button has no href on " + currentUrl());
        }
        ConsoleLog.info("Clicking", readText(tryItButton) + " -> " + editorUrl);
        driver.get(editorUrl);
        waitUntilDocumentIsReady();
        ConsoleLog.info("The editor opened in the current window", currentUrl());
        return new W3SchoolsTryItPage(driver);
    }

    private String absoluteHref(String href) {
        if (href == null || href.isEmpty()) {
            return "";
        }
        try {
            URI resolved = new URI(currentUrl()).resolve(href);
            return resolved.toString();
        } catch (URISyntaxException | IllegalArgumentException notAUri) {
            return href;
        }
    }
}
