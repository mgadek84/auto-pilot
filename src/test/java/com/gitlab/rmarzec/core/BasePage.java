package com.gitlab.rmarzec.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared behaviour for every page object: explicit waits, resilient element lookups and
 * interactions that survive the usual sources of flakiness (re-rendered DOM nodes,
 * elements covered by overlays, elements outside of the viewport).
 *
 * <p>There is no {@code Thread.sleep} anywhere in this framework - every wait is an
 * explicit {@link WebDriverWait} bound to a concrete condition.
 */
public abstract class BasePage {

    private static final String SCROLL_INTO_VIEW_SCRIPT =
            "arguments[0].scrollIntoView({block: 'center', inline: 'center'});";
    private static final String JS_CLICK_SCRIPT = "arguments[0].click();";
    private static final String DOCUMENT_READY_SCRIPT = "return document.readyState;";

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = newWait(TestConfig.explicitWait());
    }

    protected WebDriverWait newWait(Duration timeout) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, timeout);
        webDriverWait.ignoring(StaleElementReferenceException.class);
        return webDriverWait;
    }

    /* ------------------------------------------------------------------ *
     *  Waiting
     * ------------------------------------------------------------------ */

    protected void waitUntilDocumentIsReady() {
        newWait(TestConfig.explicitWait()).until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                Object state = ((JavascriptExecutor) webDriver).executeScript(DOCUMENT_READY_SCRIPT);
                return "complete".equals(state) || "interactive".equals(state);
            }

            @Override
            public String toString() {
                return "document.readyState to be interactive or complete";
            }
        });
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForPresent(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> waitForAtLeastOneVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected List<WebElement> waitForAtLeastOnePresent(By locator) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    protected void waitForUrlContaining(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /** Waits for a condition without failing the test when it never becomes true. */
    protected <T> boolean waitQuietly(Duration timeout, ExpectedCondition<T> condition) {
        try {
            newWait(timeout).until(condition);
            return true;
        } catch (TimeoutException conditionNeverMet) {
            return false;
        }
    }

    /* ------------------------------------------------------------------ *
     *  Lookups
     * ------------------------------------------------------------------ */

    /**
     * Returns the first displayed element matching any of the locators, waiting up to
     * {@code timeout} for one of them to show up. Returns {@code null} when none appears,
     * which makes it a good fit for optional UI such as cookie banners or skin-specific
     * controls.
     */
    protected WebElement findFirstDisplayed(Duration timeout, By... locators) {
        final By[] candidates = locators;
        final WebElement[] found = new WebElement[1];
        boolean appeared = waitQuietly(timeout, new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                found[0] = firstDisplayedNow(webDriver, candidates);
                return Boolean.valueOf(found[0] != null);
            }

            @Override
            public String toString() {
                return "any displayed element among " + java.util.Arrays.toString(candidates);
            }
        });
        return appeared ? found[0] : null;
    }

    protected WebElement findFirstDisplayedNow(By... locators) {
        return firstDisplayedNow(driver, locators);
    }

    private WebElement firstDisplayedNow(SearchContext context, By... locators) {
        for (By locator : locators) {
            for (WebElement element : safeFindElements(context, locator)) {
                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (StaleElementReferenceException elementWasRerendered) {
                    // The DOM changed while scanning - the next candidate is checked instead.
                }
            }
        }
        return null;
    }

    /** {@code findElements} that never throws, even when the context itself went stale. */
    protected List<WebElement> safeFindElements(SearchContext context, By locator) {
        try {
            return context.findElements(locator);
        } catch (StaleElementReferenceException | NoSuchElementException contextGone) {
            return Collections.emptyList();
        }
    }

    protected List<WebElement> safeFindElements(By locator) {
        return safeFindElements(driver, locator);
    }

    protected boolean isPresent(By locator) {
        return !safeFindElements(driver, locator).isEmpty();
    }

    /* ------------------------------------------------------------------ *
     *  Interactions
     * ------------------------------------------------------------------ */

    protected void scrollIntoView(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(SCROLL_INTO_VIEW_SCRIPT, element);
        } catch (StaleElementReferenceException elementWasRerendered) {
            // Scrolling is only an aid for the click that follows; a stale node is re-resolved there.
        }
    }

    protected void click(By locator) {
        click(waitForClickable(locator));
    }

    /**
     * Clicks an element after scrolling it into view.
     *
     * <p>Three strategies are tried in order of fidelity to a real user: a plain WebDriver
     * click, a mouse click at the element position (which reaches the element that is
     * painted on top - the pattern used by CSS-driven dropdowns) and finally a scripted
     * click for elements that stay covered by an overlay.
     */
    protected void click(WebElement element) {
        scrollIntoView(element);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
            return;
        } catch (ElementNotInteractableException | StaleElementReferenceException
                | TimeoutException clickBlocked) {
            // Handled by the fallbacks below.
        }
        try {
            new Actions(driver).moveToElement(element).click().perform();
            return;
        } catch (WebDriverException mouseClickBlocked) {
            try {
                clickWithJavaScript(element);
            } catch (StaleElementReferenceException elementWasRerendered) {
                throw elementWasRerendered;
            }
        }
    }

    protected void clickWithJavaScript(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(JS_CLICK_SCRIPT, element);
    }

    protected void type(By locator, String text) {
        WebElement input = waitForVisible(locator);
        scrollIntoView(input);
        input.clear();
        input.sendKeys(text);
    }

    protected void pressEscape() {
        try {
            waitForPresent(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (ElementNotInteractableException | StaleElementReferenceException bodyNotInteractable) {
            // Nothing to dismiss - the overlay handling below deals with the consequences.
        }
    }

    /**
     * Scrolls the window to the bottom of the page. Single page applications often leave
     * {@code document.body.scrollHeight} at zero and grow the documentElement instead, so the
     * larger of the two is used.
     */
    protected void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, Math.max(document.body.scrollHeight, document.documentElement.scrollHeight));");
    }

    /* ------------------------------------------------------------------ *
     *  Windows and frames
     * ------------------------------------------------------------------ */

    protected Set<String> windowHandles() {
        return new LinkedHashSet<String>(driver.getWindowHandles());
    }

    /**
     * Switches to the window that appeared after an action (a link with
     * {@code target="_blank"}, for example).
     *
     * @param handlesBeforeAction window handles captured before the action
     * @return {@code true} when a new window was opened and focused
     */
    protected boolean switchToNewWindow(final Set<String> handlesBeforeAction) {
        boolean opened = waitQuietly(TestConfig.shortWait(),
                ExpectedConditions.numberOfWindowsToBe(handlesBeforeAction.size() + 1));
        if (!opened) {
            return false;
        }
        try {
            for (String handle : driver.getWindowHandles()) {
                if (!handlesBeforeAction.contains(handle)) {
                    driver.switchTo().window(handle);
                    waitUntilDocumentIsReady();
                    return true;
                }
            }
        } catch (WebDriverException newWindowUnusable) {
            return false;
        }
        return false;
    }

    protected void switchToFrame(By frameLocator) {
        driver.switchTo().frame(waitForPresent(frameLocator));
    }

    protected void switchToMainDocument() {
        driver.switchTo().defaultContent();
    }

    /* ------------------------------------------------------------------ *
     *  Reading data
     * ------------------------------------------------------------------ */

    /**
     * Reads the visible text of an element and retries once when the node is replaced while
     * being read. Falls back to the {@code textContent} attribute for elements that are
     * rendered but reported as having no visible text (collapsed dropdown menus, for example).
     */
    protected String readText(WebElement element) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String visibleText = element.getText();
                if (visibleText != null && !visibleText.trim().isEmpty()) {
                    return visibleText.trim();
                }
                String textContent = element.getAttribute("textContent");
                return textContent == null ? "" : textContent.trim();
            } catch (StaleElementReferenceException elementWasRerendered) {
                if (attempt == 1) {
                    return "";
                }
            }
        }
        return "";
    }

    protected String readAttribute(WebElement element, String attributeName) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String value = element.getAttribute(attributeName);
                return value == null ? "" : value.trim();
            } catch (StaleElementReferenceException elementWasRerendered) {
                if (attempt == 1) {
                    return "";
                }
            }
        }
        return "";
    }

    /** First non-empty text found under {@code context} for any of the given locators. */
    protected String readFirstText(SearchContext context, By... locators) {
        for (By locator : locators) {
            for (WebElement element : safeFindElements(context, locator)) {
                String text = readText(element);
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    protected List<WebElement> allElements(SearchContext context, By... locators) {
        List<WebElement> elements = new ArrayList<WebElement>();
        for (By locator : locators) {
            elements.addAll(safeFindElements(context, locator));
        }
        return elements;
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
