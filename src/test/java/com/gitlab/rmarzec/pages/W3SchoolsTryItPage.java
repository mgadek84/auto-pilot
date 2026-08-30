package com.gitlab.rmarzec.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.TestConfig;
import com.gitlab.rmarzec.support.ConsentHandler;
import com.gitlab.rmarzec.support.ConsentLocators;

/**
 * The W3Schools "Try it Yourself" editor. The rendered example lives inside the
 * {@code iframeResult} frame, so every lookup here happens after switching into that frame.
 */
public class W3SchoolsTryItPage extends BasePage {

    private static final By RESULT_IFRAME = By.id("iframeResult");
    private static final By CARS_DROPDOWN = By.cssSelector("select#cars, select[name='cars'], select");

    private final ConsentHandler consentHandler;

    public W3SchoolsTryItPage(WebDriver driver) {
        super(driver);
        this.consentHandler = new ConsentHandler(driver);
    }

    /** The editor opens in a new tab, which may show the consent banner again. */
    public W3SchoolsTryItPage acceptCookiesIfPresent() {
        consentHandler.acceptIfPresent("W3Schools editor", ConsentLocators.W3SCHOOLS);
        return this;
    }

    /**
     * Switches into the frame that renders the example. Does nothing when the example is
     * already rendered in the current context.
     *
     * @return {@code true} when the driver switched into the result frame
     */
    public boolean switchToResultFrame() {
        switchToMainDocument();
        if (!waitQuietly(TestConfig.explicitWait(), ExpectedConditions.presenceOfElementLocated(RESULT_IFRAME))) {
            return false;
        }
        switchToFrame(RESULT_IFRAME);
        waitQuietly(TestConfig.explicitWait(), ExpectedConditions.presenceOfElementLocated(CARS_DROPDOWN));
        return true;
    }

    /** Header of the example, for instance "The select element". */
    public WebElement header(String expectedHeaderText) {
        By headerLocator = By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4]"
                + "[contains(normalize-space(.), '" + expectedHeaderText + "')]");
        return waitForVisible(headerLocator);
    }

    public WebElement carsDropdownElement() {
        return waitForVisible(CARS_DROPDOWN);
    }

    public Select carsDropdown() {
        return new Select(carsDropdownElement());
    }

    /**
     * Selects an option by its visible text and returns the selected option element.
     */
    public WebElement selectOptionByVisibleText(String visibleText) {
        Select dropdown = carsDropdown();
        dropdown.selectByVisibleText(visibleText);
        return dropdown.getFirstSelectedOption();
    }
}
