package com.gitlab.rmarzec.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;

/**
 * Wikipedia article page, limited to what Task 2 needs: the language selector in the
 * top-right corner and the list of interlanguage links behind it.
 *
 * <p>Wikipedia serves several skins. In Vector 2022 the languages are hidden behind a
 * dropdown button next to the article title; in the legacy Vector / Monobook skins they are
 * listed in the sidebar, optionally collapsed behind the Universal Language Selector. All of
 * those variants are handled here.
 */
public class WikipediaArticlePage extends BasePage {

    private static final By ARTICLE_HEADING = By.cssSelector("#firstHeading");
    private static final By ARTICLE_TITLE_TEXT = By.cssSelector("#firstHeading .mw-page-title-main");

    /** The language button announces how many languages exist, for example "151 języków". */
    private static final By[] LANGUAGE_COUNT_SOURCES = {
            By.cssSelector("#p-lang-btn-checkbox"),
            By.cssSelector("#p-lang-btn-label")
    };
    private static final Pattern LANGUAGE_COUNT = Pattern.compile("(\\d+)");

    /** A dropdown either opens right away or not at all, so this wait stays short. */
    private static final Duration MENU_OPEN_TIMEOUT = Duration.ofSeconds(5L);

    /**
     * Controls that reveal the language list, ordered from the newest skin to the oldest.
     *
     * <p>In Vector 2022 the control is the label of a checkbox that is painted on top of it,
     * so the label is the element to click and the click has to reach the checkbox above it.
     */
    private static final By[] LANGUAGE_BUTTONS = {
            By.cssSelector("#p-lang-btn-label"),
            By.cssSelector("#p-lang-btn button"),
            By.cssSelector("button.mw-interlanguage-selector"),
            By.cssSelector("#p-lang .uls-settings-trigger"),
            By.cssSelector("#p-lang-btn")
    };

    /**
     * Interlanguage links, ordered by the UI that renders them: the Universal Language
     * Selector dialog of current Wikipedia, the older ULS list, the Vector 2022 dropdown and
     * finally the sidebar of the legacy skins.
     */
    private static final By[] LANGUAGE_LINKS = {
            By.cssSelector(".uls-rewrite__section--all li.uls-rewrite__language-item a"),
            By.cssSelector("li.uls-rewrite__language-item a"),
            By.cssSelector(".uls-language-list a"),
            By.cssSelector("#p-lang-btn .vector-dropdown-content li.interlanguage-link > a"),
            By.cssSelector("#p-lang li.interlanguage-link > a"),
            By.cssSelector("li.interlanguage-link > a.interlanguage-link-target")
    };

    public WikipediaArticlePage(WebDriver driver) {
        super(driver);
    }

    public WikipediaArticlePage open(String url) {
        driver.get(url);
        waitForVisible(ARTICLE_HEADING);
        return this;
    }

    public String heading() {
        waitForVisible(ARTICLE_HEADING);
        WebElement titleText = findFirstDisplayedNow(ARTICLE_TITLE_TEXT);
        return titleText == null ? readText(waitForVisible(ARTICLE_HEADING)) : readText(titleText);
    }

    /**
     * Clicks the language selection button in the top-right area of the article.
     *
     * @return {@code true} when a button was clicked, {@code false} when the current skin
     *         renders the languages without any toggle (legacy sidebar).
     */
    public boolean openLanguageMenu() {
        if (languagesAreVisible()) {
            ConsoleLog.info("Languages are already listed by this skin - no button to click");
            return false;
        }

        WebElement languageButton = findFirstDisplayed(TestConfig.shortWait(), LANGUAGE_BUTTONS);
        if (languageButton == null) {
            ConsoleLog.info("No language button found in this skin");
            return false;
        }

        // The same page can offer several controls (dropdown checkbox, label, ULS trigger);
        // each candidate is clicked until the language list actually shows up.
        for (By buttonLocator : LANGUAGE_BUTTONS) {
            WebElement candidate = findFirstDisplayedNow(buttonLocator);
            if (candidate == null) {
                continue;
            }
            int announcedLanguageCount = announcedLanguageCount();
            ConsoleLog.info("Clicking the language button", describe(candidate));
            click(candidate);
            if (waitForLanguagesToBeVisible()) {
                waitForCompleteLanguageList(announcedLanguageCount);
                return true;
            }
        }
        throw new IllegalStateException("The language list did not become visible after clicking the language button");
    }

    private String describe(WebElement element) {
        String text = readText(element);
        if (!text.isEmpty()) {
            return text;
        }
        String ariaLabel = readAttribute(element, "aria-label");
        return ariaLabel.isEmpty() ? element.getTagName() : ariaLabel;
    }

    private boolean languagesAreVisible() {
        return findFirstDisplayedNow(LANGUAGE_LINKS) != null;
    }

    private boolean waitForLanguagesToBeVisible() {
        for (By languageLinks : LANGUAGE_LINKS) {
            if (waitQuietly(MENU_OPEN_TIMEOUT, ExpectedConditions.visibilityOfElementLocated(languageLinks))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The language dialog renders its entries progressively, so reading the list immediately
     * after it opens can return only part of it. This waits for the announced number of
     * languages and, when the page announces no number, for the list to stop growing.
     */
    private void waitForCompleteLanguageList(final int announcedLanguageCount) {
        if (announcedLanguageCount > 0) {
            waitQuietly(MENU_OPEN_TIMEOUT, new ExpectedCondition<Boolean>() {
                @Override
                public Boolean apply(WebDriver webDriver) {
                    return Boolean.valueOf(languages().size() >= announcedLanguageCount);
                }

                @Override
                public String toString() {
                    return "all " + announcedLanguageCount + " languages to be rendered";
                }
            });
            return;
        }
        waitQuietly(MENU_OPEN_TIMEOUT, new ExpectedCondition<Boolean>() {
            private int previousCount = -1;

            @Override
            public Boolean apply(WebDriver webDriver) {
                int currentCount = languages().size();
                boolean stable = currentCount > 0 && currentCount == previousCount;
                previousCount = currentCount;
                return Boolean.valueOf(stable);
            }

            @Override
            public String toString() {
                return "the number of rendered languages to stop growing";
            }
        });
    }

    /** Number of languages announced by the language button, or 0 when it announces none. */
    private int announcedLanguageCount() {
        for (By source : LANGUAGE_COUNT_SOURCES) {
            for (WebElement element : safeFindElements(source)) {
                String announcement = readAttribute(element, "aria-label");
                if (announcement.isEmpty()) {
                    announcement = readText(element);
                }
                Matcher matcher = LANGUAGE_COUNT.matcher(announcement);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }
        return 0;
    }

    /**
     * All languages the article is available in.
     *
     * <p>The elements are the interlanguage anchors themselves, so the caller can read both
     * the display name and the {@code href} of every language.
     */
    public List<WebElement> languages() {
        for (By languageLinks : LANGUAGE_LINKS) {
            List<WebElement> found = safeFindElements(languageLinks);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return new ArrayList<WebElement>();
    }

    /**
     * Display name of a language entry. Vector 2022 wraps the name in a {@code <span>} and
     * hides the anchor text when the dropdown is collapsed, hence the attribute fallbacks.
     */
    public String languageName(WebElement languageLink) {
        String text = readText(languageLink);
        if (!text.isEmpty()) {
            return text;
        }
        String title = readAttribute(languageLink, "title");
        if (!title.isEmpty()) {
            return title;
        }
        return readAttribute(languageLink, "lang");
    }

    public String languageUrl(WebElement languageLink) {
        return readAttribute(languageLink, "href");
    }

    public boolean isEnglish(WebElement languageLink) {
        String name = languageName(languageLink);
        String languageCode = readAttribute(languageLink, "lang");
        if (languageCode.isEmpty()) {
            languageCode = readAttribute(languageLink, "hreflang");
        }
        return "English".equalsIgnoreCase(name) || "en".equalsIgnoreCase(languageCode);
    }
}
