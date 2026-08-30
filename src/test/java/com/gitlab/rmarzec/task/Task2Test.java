package com.gitlab.rmarzec.task;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import com.gitlab.rmarzec.core.BaseTest;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.pages.WikipediaArticlePage;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Task 2 - list the languages a Wikipedia article is available in and print the URL of the
 * English version.
 */
public class Task2Test extends BaseTest {

    private static final String WIKI_ARTICLE_URL = "https://pl.wikipedia.org/wiki/Wiki";
    private static final String ENGLISH_WIKIPEDIA_HOST = "en.wikipedia.org";

    @Test(description = "Task 2 - print every available language of the article and the English URL")
    public void printsAvailableLanguagesAndEnglishUrl() {
        WikipediaArticlePage articlePage = new WikipediaArticlePage(driver).open(WIKI_ARTICLE_URL);

        ConsoleLog.step("Task 2 - opened article: " + articlePage.heading());
        articlePage.openLanguageMenu();

        List<WebElement> languages = articlePage.languages();
        assertFalse(languages.isEmpty(), "The article must expose at least one language link");
        ConsoleLog.step("Task 2 - available languages (" + languages.size() + ")");

        String englishUrl = null;
        for (WebElement language : languages) {
            String languageName = articlePage.languageName(language);
            ConsoleLog.info(languageName);

            if (articlePage.isEnglish(language)) {
                englishUrl = articlePage.languageUrl(language);
                ConsoleLog.info("English version URL", englishUrl);
            }
        }

        assertNotNull(englishUrl, "The English version of the article was not found in the language list");
        assertTrue(englishUrl.contains(ENGLISH_WIKIPEDIA_HOST),
                "The English link should point to " + ENGLISH_WIKIPEDIA_HOST + " but was: " + englishUrl);
    }
}
