package com.gitlab.rmarzec.pages;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.gitlab.rmarzec.core.BasePage;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.TestConfig;
import com.gitlab.rmarzec.model.YTTile;

/**
 * YouTube search results.
 *
 * <p>YouTube serves two generations of markup for search results - the classic
 * {@code ytd-video-renderer} custom elements and the newer {@code yt-lockup-view-model}
 * ones - and both are handled here. Shorts shelves, playlists, mixes, channel cards and ads
 * are filtered out so that only standard video results are collected.
 */
public class YouTubeSearchResultsPage extends BasePage {

    private static final By RESULTS_SECTION = By.cssSelector("ytd-item-section-renderer, ytd-section-list-renderer");

    private static final By[] VIDEO_TILES = {
            By.cssSelector("ytd-item-section-renderer ytd-video-renderer"),
            By.cssSelector("ytd-video-renderer"),
            By.cssSelector("ytd-item-section-renderer yt-lockup-view-model"),
            By.cssSelector("yt-lockup-view-model")
    };

    private static final By[] TITLE_LINKS = {
            By.cssSelector("a#video-title-link"),
            By.cssSelector("a#video-title"),
            By.cssSelector("h3 a#video-title-link"),
            By.cssSelector("a.yt-lockup-metadata-view-model__title"),
            By.cssSelector("h3 a")
    };

    private static final By[] TITLE_TEXTS = {
            By.cssSelector("#video-title"),
            By.cssSelector("h3 span.yt-core-attributed-string"),
            By.cssSelector("h3")
    };

    private static final By[] CHANNEL_NAMES = {
            By.cssSelector("ytd-channel-name a"),
            By.cssSelector("#channel-name a"),
            By.cssSelector("ytd-channel-name #text"),
            By.cssSelector(".yt-content-metadata-view-model__metadata-row a[href^='/@']"),
            By.cssSelector("a[href^='/@']"),
            By.cssSelector("a[href*='/channel/']")
    };

    private static final By[] DURATION_BADGES = {
            By.cssSelector("ytd-thumbnail-overlay-time-status-renderer badge-shape"),
            By.cssSelector("badge-shape.ytBadgeShapeThumbnailBadge"),
            By.cssSelector("ytd-thumbnail-overlay-time-status-renderer #text"),
            By.cssSelector("ytd-thumbnail-overlay-time-status-renderer span"),
            By.cssSelector("badge-shape div.badge-shape-wiz__text"),
            By.cssSelector("thumbnail-overlay-badge-view-model badge-shape")
    };

    private static final By[] LIVE_MARKERS = {
            By.cssSelector("ytd-thumbnail[is-live-video]"),
            By.cssSelector("[is-live-video]"),
            By.cssSelector("badge-shape.ytBadgeShapeLive"),
            By.cssSelector("ytd-thumbnail-overlay-time-status-renderer[overlay-style='LIVE']"),
            By.cssSelector("badge-shape.badge-shape-wiz--thumbnail-live"),
            By.cssSelector("ytd-badge-supported-renderer .badge-style-type-live-now-alternate")
    };

    /** Live streams show how many people are watching instead of a view count and a date. */
    private static final By[] METADATA_LINES = {
            By.cssSelector("#metadata-line"),
            By.cssSelector(".yt-content-metadata-view-model__metadata-row")
    };

    /** Markers of tiles that are not standard videos and must be skipped. */
    private static final By[] NON_VIDEO_MARKERS = {
            By.cssSelector("yt-collection-thumbnail-view-model"),
            By.cssSelector("yt-collections-stack"),
            By.cssSelector("ytd-thumbnail-overlay-bottom-panel-renderer"),
            By.cssSelector("ytm-shorts-lockup-view-model")
    };

    private static final String[] LIVE_KEYWORDS = {"live", "na żywo"};
    private static final String[] WATCHING_NOW_KEYWORDS = {"watching", "oglądając", "widz"};
    private static final String[] SPONSORED_KEYWORDS = {"sponsored", "sponsorowane", "reklama", "ad ·"};

    private static final int MAX_SCROLL_ATTEMPTS = 8;

    public YouTubeSearchResultsPage(WebDriver driver) {
        super(driver);
    }

    public YouTubeSearchResultsPage waitUntilResultsAreListed() {
        waitForUrlContaining("/results");
        waitQuietly(TestConfig.explicitWait(), ExpectedConditions.presenceOfElementLocated(RESULTS_SECTION));
        waitForTileCount(1);
        return this;
    }

    /**
     * Collects the first {@code expectedCount} standard video results, scrolling the feed
     * until enough tiles are rendered.
     */
    public List<YTTile> collectVideoTiles(int expectedCount) {
        List<YTTile> tiles = readTiles(expectedCount);
        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS && tiles.size() < expectedCount; attempt++) {
            int renderedBeforeScroll = rawTiles().size();
            scrollToBottom();
            boolean moreTilesRendered = waitForTileCount(renderedBeforeScroll + 1);
            tiles = readTiles(expectedCount);
            if (!moreTilesRendered && tiles.size() < expectedCount) {
                ConsoleLog.info("No further results were rendered while scrolling",
                        "collected " + tiles.size() + " of " + expectedCount);
                break;
            }
        }
        return tiles;
    }

    private boolean waitForTileCount(final int minimumCount) {
        return waitQuietly(TestConfig.explicitWait(), new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                return Boolean.valueOf(rawTiles().size() >= minimumCount);
            }

            @Override
            public String toString() {
                return "at least " + minimumCount + " result tiles to be rendered";
            }
        });
    }

    /** All result containers of both markup generations, in the order they appear on the page. */
    private List<WebElement> rawTiles() {
        for (By tileLocator : VIDEO_TILES) {
            List<WebElement> found = safeFindElements(tileLocator);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return new ArrayList<WebElement>();
    }

    private List<YTTile> readTiles(int expectedCount) {
        List<YTTile> tiles = new ArrayList<YTTile>();
        for (WebElement rawTile : rawTiles()) {
            if (tiles.size() >= expectedCount) {
                break;
            }
            YTTile tile = readTile(rawTile);
            if (tile != null) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    /**
     * Maps a result container to a {@link YTTile}, or returns {@code null} when the container
     * is not a standard video (Shorts shelf, playlist, mix, channel card or ad).
     */
    private YTTile readTile(WebElement rawTile) {
        String videoUrl = readFirstAttribute(rawTile, "href", TITLE_LINKS);
        if (!isStandardVideoUrl(videoUrl) || isNonVideoTile(rawTile) || isAdvertisement(rawTile)) {
            return null;
        }

        String title = title(rawTile);
        if (title.isEmpty()) {
            return null;
        }
        String duration = duration(rawTile);
        if (duration.isEmpty()) {
            // Scheduled premieres and upcoming streams carry no length yet, so they are not
            // standard video results either.
            ConsoleLog.info("Skipped a result without a duration", title);
            return null;
        }
        String channelName = readFirstText(rawTile, CHANNEL_NAMES);
        if (channelName.isEmpty()) {
            // Either the tile is still rendering or it was replaced while being read; it is
            // picked up again on the next pass instead of being reported half filled.
            ConsoleLog.info("Skipped a result without a channel name", title);
            return null;
        }
        return new YTTile(title, channelName, duration);
    }

    private String title(WebElement rawTile) {
        String title = readFirstText(rawTile, TITLE_TEXTS);
        if (!title.isEmpty()) {
            return title;
        }
        title = readFirstAttribute(rawTile, "title", TITLE_LINKS);
        if (!title.isEmpty()) {
            return title;
        }
        return readFirstAttribute(rawTile, "aria-label", TITLE_LINKS);
    }

    /**
     * Duration shown on the thumbnail, or {@link YTTile#LIVE_DURATION} for live streams.
     */
    private String duration(WebElement rawTile) {
        String badgeText = firstLineOf(readFirstText(rawTile, DURATION_BADGES));
        if (isLive(rawTile, badgeText)) {
            return YTTile.LIVE_DURATION;
        }
        return badgeText;
    }

    private boolean isLive(WebElement rawTile, String badgeText) {
        if (containsAnyKeyword(badgeText, LIVE_KEYWORDS)) {
            return true;
        }
        for (By liveMarker : LIVE_MARKERS) {
            if (!safeFindElements(rawTile, liveMarker).isEmpty()) {
                return true;
            }
        }
        // A tile with no length that reports how many people are watching is a live stream.
        return badgeText.isEmpty()
                && containsAnyKeyword(readFirstText(rawTile, METADATA_LINES), WATCHING_NOW_KEYWORDS);
    }

    /** The duration badge repeats its value for screen readers, so only the first line counts. */
    private String firstLineOf(String text) {
        int newLine = text.indexOf('\n');
        return newLine < 0 ? text.trim() : text.substring(0, newLine).trim();
    }

    private boolean isStandardVideoUrl(String videoUrl) {
        return videoUrl != null && videoUrl.contains("/watch?v=") && !videoUrl.contains("/shorts/");
    }

    private boolean isNonVideoTile(WebElement rawTile) {
        for (By marker : NON_VIDEO_MARKERS) {
            if (!safeFindElements(rawTile, marker).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdvertisement(WebElement rawTile) {
        String tagName = tagNameOf(rawTile);
        if (tagName.contains("ad-slot") || tagName.contains("promoted") || tagName.contains("in-feed-ad")) {
            return true;
        }
        String badges = readFirstText(rawTile, By.cssSelector("ytd-badge-supported-renderer"),
                By.cssSelector(".yt-badge-shape__text"));
        return containsAnyKeyword(badges, SPONSORED_KEYWORDS);
    }

    private String tagNameOf(WebElement element) {
        try {
            String tagName = element.getTagName();
            return tagName == null ? "" : tagName.toLowerCase();
        } catch (RuntimeException elementWasRerendered) {
            return "";
        }
    }

    private String readFirstAttribute(WebElement context, String attributeName, By... locators) {
        for (By locator : locators) {
            for (WebElement element : safeFindElements(context, locator)) {
                String value = readAttribute(element, attributeName);
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean containsAnyKeyword(String text, String[] keywords) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String normalized = text.toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
