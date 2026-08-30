package com.gitlab.rmarzec.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;
import com.gitlab.rmarzec.support.ConsentHandler;
import com.gitlab.rmarzec.support.ConsentLocators;

/**
 * Google home page: cookie consent, the search box and the "Szczęśliwy traf"
 * (I'm Feeling Lucky) button.
 */
public class GoogleSearchPage extends BasePage {

    private static final String GOOGLE_URL = "https://www.google.com/";

    /** Google renders the query field as a textarea on desktop and as an input on some layouts. */
    private static final By[] SEARCH_BOX = {
            By.cssSelector("textarea[name='q']"),
            By.cssSelector("input[name='q']")
    };

    private static final By[] FEELING_LUCKY_BUTTONS = {
            By.cssSelector("input[name='btnI']"),
            By.cssSelector("input[value='Szczęśliwy traf']"),
            By.xpath("//input[@name='btnI' or contains(@value, 'Szczęśliwy') or contains(@value, 'Feeling Lucky')]"),
            By.xpath("//*[@role='button'][contains(., 'Szczęśliwy traf') or contains(., 'Feeling Lucky')]")
    };

    /** The autocomplete overlay that covers the buttons once a query has been typed. */
    private static final By SUGGESTIONS_OVERLAY = By.cssSelector("ul[role='listbox']");

    private final ConsentHandler consentHandler;

    public GoogleSearchPage(WebDriver driver) {
        super(driver);
        this.consentHandler = new ConsentHandler(driver);
    }

    public GoogleSearchPage open() {
        driver.get(GOOGLE_URL);
        waitUntilDocumentIsReady();
        return this;
    }

    public GoogleSearchPage acceptCookiesIfPresent() {
        consentHandler.acceptIfPresent("Google", ConsentLocators.GOOGLE);
        return this;
    }

    public GoogleSearchPage typeQuery(String query) {
        WebElement searchBox = findFirstDisplayed(TestConfig.explicitWait(), SEARCH_BOX);
        if (searchBox == null) {
            throw new IllegalStateException("The Google search box was not found");
        }
        scrollIntoView(searchBox);
        searchBox.clear();
        searchBox.sendKeys(query);
        ConsoleLog.info("Typed query", query);
        return this;
    }

    /**
     * Clicks "Szczęśliwy traf". The autocomplete dropdown opens on top of the button while
     * typing, so it is dismissed first and the click falls back to JavaScript when the
     * overlay is still in the way.
     */
    public void clickFeelingLucky() {
        dismissSuggestionsOverlay();

        WebElement feelingLucky = findFirstDisplayed(TestConfig.shortWait(), FEELING_LUCKY_BUTTONS);
        if (feelingLucky == null) {
            // The button is present but hidden on some layouts; a scripted click still submits the form.
            WebElement hiddenButton = firstPresentFeelingLuckyButton();
            if (hiddenButton == null) {
                throw new IllegalStateException("The 'Szczęśliwy traf' button was not found on the Google home page");
            }
            ConsoleLog.info("'Szczęśliwy traf' button is hidden - clicking it with JavaScript");
            clickWithJavaScript(hiddenButton);
            return;
        }
        ConsoleLog.info("Clicking button", readAttribute(feelingLucky, "value"));
        click(feelingLucky);
    }

    private WebElement firstPresentFeelingLuckyButton() {
        for (By locator : FEELING_LUCKY_BUTTONS) {
            for (WebElement candidate : safeFindElements(locator)) {
                return candidate;
            }
        }
        return null;
    }

    private void dismissSuggestionsOverlay() {
        if (!isPresent(SUGGESTIONS_OVERLAY)) {
            return;
        }
        pressEscape();
        waitQuietly(TestConfig.shortWait(), ExpectedConditions.invisibilityOfElementLocated(SUGGESTIONS_OVERLAY));
    }

    /** Waits for the browser to leave Google after the redirect, without failing if it does not. */
    public boolean waitForNavigationAwayFromGoogle() {
        return waitQuietly(TestConfig.explicitWait(), new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                String url = webDriver.getCurrentUrl();
                return Boolean.valueOf(url != null && !url.contains("google.com") && !url.contains("google.pl"));
            }

            @Override
            public String toString() {
                return "the browser to be redirected away from Google";
            }
        });
    }
}
