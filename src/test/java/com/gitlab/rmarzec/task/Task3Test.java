package com.gitlab.rmarzec.task;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import com.gitlab.rmarzec.core.BaseTest;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.pages.GoogleSearchPage;
import com.gitlab.rmarzec.pages.W3SchoolsTagPage;
import com.gitlab.rmarzec.pages.W3SchoolsTryItPage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Task 3 - reach the W3Schools documentation of the {@code <select>} tag through Google's
 * "Szczęśliwy traf" (I'm Feeling Lucky) button and drive the example in the
 * "Try it Yourself" editor.
 */
public class Task3Test extends BaseTest {

    private static final String SEARCH_QUERY = "HTML select tag - W3Schools";
    private static final String EXPECTED_URL = "https://www.w3schools.com/tags/tag_select.asp";
    private static final String TRYIT_URL = "https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_select";
    private static final String EXPECTED_HEADER = "The select element";
    private static final String OPTION_TO_SELECT = "Opel";

    @Test(description = "Task 3 - select 'Opel' in the W3Schools <select> example reached via Google")
    public void selectsOpelInTheW3SchoolsSelectExample() {
        GoogleSearchPage googlePage = new GoogleSearchPage(driver).open();
        ConsoleLog.step("Task 3 - Google");
        googlePage.acceptCookiesIfPresent();
        googlePage.typeQuery(SEARCH_QUERY);
        googlePage.clickFeelingLucky();
        googlePage.waitForNavigationAwayFromGoogle();

        W3SchoolsTagPage tagPage = openSelectTagPage();
        ConsoleLog.step("Task 3 - W3Schools");
        tagPage.acceptCookiesIfPresent();

        W3SchoolsTryItPage editorPage = openEditor(tagPage);

        WebElement header = editorPage.header(EXPECTED_HEADER);
        String headerText = header.getText();
        ConsoleLog.info("Header", headerText);
        assertEquals(headerText.trim(), EXPECTED_HEADER, "Unexpected header of the example");

        WebElement selectedOption = editorPage.selectOptionByVisibleText(OPTION_TO_SELECT);
        String selectedText = selectedOption.getText().trim();
        String selectedValue = selectedOption.getAttribute("value");
        ConsoleLog.info("Selected option", selectedText + ", " + selectedValue);

        assertEquals(selectedText, OPTION_TO_SELECT, "A different option ended up selected");
        assertEquals(selectedValue, OPTION_TO_SELECT.toLowerCase(), "Unexpected value of the selected option");
    }

    /**
     * Step 5 of the task: the "Szczęśliwy traf" redirect is not guaranteed to land on the
     * expected page, so the current URL is verified, logged and corrected when needed.
     */
    private W3SchoolsTagPage openSelectTagPage() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl != null && currentUrl.startsWith(EXPECTED_URL)) {
            ConsoleLog.info("'Szczęśliwy traf' landed on the expected page", currentUrl);
            return new W3SchoolsTagPage(driver);
        }

        ConsoleLog.info("Unexpected URL after 'Szczęśliwy traf'", String.valueOf(currentUrl));
        ConsoleLog.info("Navigating directly to", EXPECTED_URL);
        W3SchoolsTagPage tagPage = new W3SchoolsTagPage(driver).open(EXPECTED_URL);
        assertTrue(driver.getCurrentUrl().startsWith(EXPECTED_URL),
                "The browser should be on " + EXPECTED_URL + " but is on " + driver.getCurrentUrl());
        return tagPage;
    }

    /**
     * Opens the Try it Yourself editor and recovers from a dead Chrome session. The editor
     * page is ad-heavy and headless Chrome sometimes dies while it is still loading; the
     * example itself can then be reached in a fresh window.
     */
    private W3SchoolsTryItPage openEditor(W3SchoolsTagPage tagPage) {
        try {
            W3SchoolsTryItPage editorPage = tagPage.clickFirstTryItYourself();
            editorPage.acceptCookiesIfPresent();
            boolean switchedToFrame = editorPage.switchToResultFrame();
            ConsoleLog.info("Example rendered in the result iframe", String.valueOf(switchedToFrame));
            return editorPage;
        } catch (WebDriverException browserDied) {
            ConsoleLog.info("Browser session died on the editor page - retrying in a fresh window");
            restartBrowser();
            W3SchoolsTryItPage editorPage = new W3SchoolsTryItPage(driver).open(TRYIT_URL);
            editorPage.acceptCookiesIfPresent();
            boolean switchedToFrame = editorPage.switchToResultFrame();
            ConsoleLog.info("Example rendered in the result iframe", String.valueOf(switchedToFrame));
            return editorPage;
        }
    }
}
