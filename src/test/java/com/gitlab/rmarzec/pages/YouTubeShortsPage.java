package com.gitlab.rmarzec.pages;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.TestConfig;

/**
 * YouTube Shorts player.
 *
 * <p>The feed keeps one rendered reel for the video that is playing plus placeholders for the
 * reels that follow. The playing reel is identified by the video id in the address bar, so the
 * channel name is always read from the reel that belongs to the current video and never from a
 * neighbouring one.
 */
public class YouTubeShortsPage extends BasePage {

    private static final By SHORTS_CONTAINER =
            By.cssSelector("ytd-shorts, ytd-reel-video-renderer, #shorts-container");

    /** Reel containers, newest markup first. */
    private static final By[] REEL_CONTAINERS = {
            By.cssSelector("ytd-reel-video-renderer[is-active]"),
            By.cssSelector("ytd-reel-video-renderer"),
            By.cssSelector("#shorts-container")
    };

    /** Channel link inside a reel. */
    private static final By[] CHANNEL_NAMES_IN_REEL = {
            By.cssSelector("ytd-channel-name a"),
            By.cssSelector("#channel-name a"),
            By.cssSelector("yt-reel-channel-bar-view-model a"),
            By.cssSelector("a[href^='/@']"),
            By.cssSelector("a[href*='/channel/']")
    };

    private static final Pattern SHORTS_VIDEO_ID = Pattern.compile("/shorts/([\\w-]+)");
    private static final String[] UNAVAILABLE_KEYWORDS = {"video unavailable", "film niedostępny", "niedostępny"};

    public YouTubeShortsPage(WebDriver driver) {
        super(driver);
    }

    public YouTubeShortsPage waitUntilLoaded() {
        waitForUrlContaining("/shorts");
        waitQuietly(TestConfig.explicitWait(), ExpectedConditions.presenceOfElementLocated(SHORTS_CONTAINER));
        return this;
    }

    /** Id of the Short that is currently open, taken from the address bar. */
    public String currentlyPlayingVideoId() {
        Matcher matcher = SHORTS_VIDEO_ID.matcher(String.valueOf(currentUrl()));
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Channel name of the Shorts video that is currently playing, including the {@code @}
     * handle.
     *
     * @return the channel name, or an empty string when Shorts served no reel at all
     */
    public String currentlyPlayingChannelName() {
        WebElement reel = waitForPlayingReel();
        if (reel == null) {
            return "";
        }
        return readFirstText(reel, CHANNEL_NAMES_IN_REEL);
    }

    /**
     * Whether the player reports the Short as not playable. Automated browser sessions are
     * regularly refused playback by YouTube while the reel metadata (channel, title,
     * reactions) still renders, and the tasks report that instead of pretending the video
     * plays.
     */
    public boolean isPlaybackUnavailable() {
        WebElement reel = findFirstDisplayedNow(REEL_CONTAINERS);
        if (reel == null) {
            return false;
        }
        String reelText = readText(reel).toLowerCase();
        for (String keyword : UNAVAILABLE_KEYWORDS) {
            if (reelText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for the reel of the current video to render its channel bar, which appears later
     * than the reel container itself.
     */
    private WebElement waitForPlayingReel() {
        final WebElement[] playingReel = new WebElement[1];
        waitQuietly(TestConfig.explicitWait(), new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                WebElement reel = reelContaining(currentlyPlayingVideoId());
                playingReel[0] = reel == null ? firstReelWithChannelLink() : reel;
                return Boolean.valueOf(playingReel[0] != null);
            }

            @Override
            public String toString() {
                return "the reel of the current Short to render its channel name";
            }
        });
        return playingReel[0];
    }

    /** The reel that links to the given video id, which is the one being played. */
    private WebElement reelContaining(String videoId) {
        if (videoId.isEmpty()) {
            return null;
        }
        By linkToCurrentVideo = By.cssSelector("a[href*='" + videoId + "']");
        for (WebElement reel : allReels()) {
            if (!safeFindElements(reel, linkToCurrentVideo).isEmpty()
                    && !readFirstText(reel, CHANNEL_NAMES_IN_REEL).isEmpty()) {
                return reel;
            }
        }
        return null;
    }

    private WebElement firstReelWithChannelLink() {
        for (WebElement reel : allReels()) {
            if (!readFirstText(reel, CHANNEL_NAMES_IN_REEL).isEmpty()) {
                return reel;
            }
        }
        return null;
    }

    private List<WebElement> allReels() {
        for (By reelContainer : REEL_CONTAINERS) {
            List<WebElement> reels = safeFindElements(reelContainer);
            if (!reels.isEmpty()) {
                return reels;
            }
        }
        return Collections.emptyList();
    }
}
