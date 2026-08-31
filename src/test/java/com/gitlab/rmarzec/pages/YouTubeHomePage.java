package com.gitlab.rmarzec.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;
import com.gitlab.rmarzec.support.ConsentHandler;
import com.gitlab.rmarzec.support.ConsentLocators;

/**
 * YouTube home page: consent banner, the guide entry leading to Shorts, the logo that brings
 * the user back home and the search box.
 */
public class YouTubeHomePage extends BasePage {

    private static final String YOUTUBE_URL = "https://www.youtube.com/";
    private static final String SHORTS_URL = "https://www.youtube.com/shorts";

    private static final By[] SEARCH_INPUTS = {
            By.cssSelector("input#search"),
            By.cssSelector("input[name='search_query']"),
            By.cssSelector("ytd-searchbox input"),
            By.cssSelector("div#center input")
    };

    private static final By[] SHORTS_ENTRIES = {
            By.cssSelector("ytd-guide-entry-renderer a#endpoint[title='Shorts']"),
            By.cssSelector("ytd-mini-guide-entry-renderer a[title='Shorts']"),
            By.cssSelector("a#endpoint[href='/shorts']"),
            By.cssSelector("a[href^='/shorts']"),
            By.xpath("//a[.//*[normalize-space()='Shorts'] or @title='Shorts']")
    };

    private static final By[] LOGO_LINKS = {
            By.cssSelector("ytd-topbar-logo-renderer a#logo"),
            By.cssSelector("a#logo"),
            By.cssSelector("a[href='/'][title='YouTube - strona główna']"),
            By.cssSelector("a[title*='YouTube']")
    };

    private static final By HOME_FEED = By.cssSelector("ytd-browse[page-subtype='home'], ytd-rich-grid-renderer");

    private final ConsentHandler consentHandler;

    public YouTubeHomePage(WebDriver driver) {
        super(driver);
        this.consentHandler = new ConsentHandler(driver);
    }

    public YouTubeHomePage open() {
        driver.get(YOUTUBE_URL);
        waitUntilDocumentIsReady();
        return this;
    }

    public YouTubeHomePage acceptCookiesIfPresent() {
        consentHandler.acceptIfPresent("YouTube", ConsentLocators.YOUTUBE);
        waitQuietly(TestConfig.shortWait(), ExpectedConditions.presenceOfElementLocated(HOME_FEED));
        return this;
    }

    /**
     * Opens the Shorts tab from the navigation guide, falling back to the Shorts URL when the
     * guide entry is not rendered (narrow viewports hide it behind the hamburger menu).
     */
    public YouTubeShortsPage openShorts() {
        WebElement shortsEntry = findFirstDisplayed(TestConfig.shortWait(), SHORTS_ENTRIES);
        if (shortsEntry == null) {
            ConsoleLog.info("Shorts entry not visible in the guide - navigating to", SHORTS_URL);
            driver.get(SHORTS_URL);
        } else {
            try {
                ConsoleLog.info("Clicking the Shorts tab", describeLink(shortsEntry));
                click(shortsEntry);
            } catch (WebDriverException clickFailed) {
                ConsoleLog.info("Shorts tab click failed - navigating to", SHORTS_URL);
                driver.get(SHORTS_URL);
            }
        }
        if (!String.valueOf(currentUrl()).contains("/shorts")
                && !waitQuietly(TestConfig.shortWait(), ExpectedConditions.urlContains("/shorts"))) {
            ConsoleLog.info("Still not on Shorts - navigating to", SHORTS_URL);
            driver.get(SHORTS_URL);
        }
        return new YouTubeShortsPage(driver).waitUntilLoaded();
    }

    private String describeLink(WebElement link) {
        String href = readAttribute(link, "href");
        if (!href.isEmpty()) {
            return href;
        }
        String title = readAttribute(link, "title");
        return title.isEmpty() ? readText(link) : title;
    }

    /** Goes back to the home page by clicking the YouTube logo. */
    public YouTubeHomePage goHome() {
        WebElement logo = findFirstDisplayed(TestConfig.shortWait(), LOGO_LINKS);
        if (logo == null) {
            ConsoleLog.info("YouTube logo not clickable - navigating to", YOUTUBE_URL);
            driver.get(YOUTUBE_URL);
        } else {
            click(logo);
        }
        waitQuietly(TestConfig.explicitWait(), ExpectedConditions.presenceOfElementLocated(HOME_FEED));
        ConsoleLog.info("Back on the YouTube home page", currentUrl());
        return this;
    }

    public YouTubeSearchResultsPage searchFor(String keyword) {
        WebElement searchInput = findFirstDisplayed(TestConfig.explicitWait(), SEARCH_INPUTS);
        if (searchInput == null) {
            throw new IllegalStateException("The YouTube search box was not found on " + currentUrl());
        }
        scrollIntoView(searchInput);
        searchInput.clear();
        searchInput.sendKeys(keyword);
        searchInput.sendKeys(Keys.ENTER);
        ConsoleLog.info("Searching for", keyword);

        return new YouTubeSearchResultsPage(driver).waitUntilResultsAreListed();
    }
}
