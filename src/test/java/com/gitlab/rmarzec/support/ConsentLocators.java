package com.gitlab.rmarzec.support;

import org.openqa.selenium.By;

/**
 * Locators of the "accept cookies" buttons of the sites used by the tasks, kept in one place
 * because several page objects of the same site share them.
 *
 * <p>Every site is described by more than one locator: the stable id first, then structural
 * selectors, then a text based fallback covering the Polish and the English UI.
 */
public final class ConsentLocators {

    public static final By[] GOOGLE = {
            By.id("L2AGLb"),
            By.cssSelector("button#L2AGLb"),
            By.xpath("//button[normalize-space()='Zaakceptuj wszystko' or normalize-space()='Accept all'"
                    + " or normalize-space()='Zgadzam się' or normalize-space()='I agree']"),
            By.xpath("//div[@role='button'][normalize-space()='Zaakceptuj wszystko'"
                    + " or normalize-space()='Accept all']")
    };

    public static final By[] W3SCHOOLS = {
            By.id("accept-choices"),
            By.cssSelector("button#accept-choices"),
            By.cssSelector("button.fc-cta-consent"),
            By.cssSelector("#snigel-cmp-framework button[data-action='accept']"),
            By.xpath("//button[contains(., 'Accept all') or contains(., 'Accept All')"
                    + " or contains(., 'I accept') or contains(., 'Accept & continue')]")
    };

    public static final By[] YOUTUBE = {
            By.cssSelector("tp-yt-paper-dialog button[aria-label*='Zaakceptuj']"),
            By.cssSelector("tp-yt-paper-dialog button[aria-label*='Accept']"),
            By.cssSelector("ytd-consent-bump-v2-lightbox button[aria-label*='Accept']"),
            By.xpath("//tp-yt-paper-dialog//*[self::button or @role='button']"
                    + "[contains(., 'Zaakceptuj wszystko') or contains(., 'Accept all')]"),
            By.xpath("//*[self::button or @role='button']"
                    + "[contains(., 'Zaakceptuj wszystko') or contains(., 'Accept all')"
                    + " or contains(., 'Zgadzam się') or contains(., 'I agree')]"),
            By.xpath("//form[contains(@action, 'consent')]//button")
    };

    private ConsentLocators() {
    }
}
