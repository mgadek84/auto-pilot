package com.gitlab.rmarzec.task;

import java.util.List;

import org.testng.annotations.Test;

import com.gitlab.rmarzec.core.BaseTest;
import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.model.YTTile;
import com.gitlab.rmarzec.pages.YouTubeHomePage;
import com.gitlab.rmarzec.pages.YouTubeSearchResultsPage;
import com.gitlab.rmarzec.pages.YouTubeShortsPage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Task 4 - visit Shorts, come back to the home page, search for "Live" and collect the first
 * twelve standard video results into a list of {@link YTTile}.
 */
public class Task4Test extends BaseTest {

    private static final String SEARCH_KEYWORD = "Live";
    private static final int EXPECTED_TILE_COUNT = 12;

    @Test(description = "Task 4 - collect the first 12 video results for the keyword 'Live'")
    public void collectsFirstTwelveVideoResultsForLiveKeyword() {
        YouTubeHomePage homePage = new YouTubeHomePage(driver).open().acceptCookiesIfPresent();
        ConsoleLog.step("Task 4 - YouTube home page opened");

        YouTubeShortsPage shortsPage = homePage.openShorts();
        ConsoleLog.step("Task 4 - Shorts");
        ConsoleLog.info("Video id of the current Short", shortsPage.currentlyPlayingVideoId());
        String shortsChannelName = shortsPage.currentlyPlayingChannelName();
        ConsoleLog.info("Channel of the currently playing Short", shortsChannelName);
        if (shortsPage.isPlaybackUnavailable()) {
            ConsoleLog.info("The player refuses playback in this session"
                    + " - the channel above comes from the reel metadata");
        }
        assertFalse(shortsChannelName.isEmpty(), "No channel name was found for the current Short");

        homePage.goHome();

        YouTubeSearchResultsPage resultsPage = homePage.searchFor(SEARCH_KEYWORD);
        List<YTTile> tiles = resultsPage.collectVideoTiles(EXPECTED_TILE_COUNT);

        ConsoleLog.step("Task 4 - collected " + tiles.size() + " video tiles for '" + SEARCH_KEYWORD + "'");
        for (YTTile tile : tiles) {
            ConsoleLog.info(tile.toString());
        }

        assertFalse(tiles.isEmpty(), "No standard video results were collected for '" + SEARCH_KEYWORD + "'");
        assertEquals(tiles.size(), EXPECTED_TILE_COUNT,
                "Wrong number of standard video results collected for '" + SEARCH_KEYWORD + "'");
        assertNoTileIsIncomplete(tiles);

        ConsoleLog.step("Task 4 - videos that are not live streams");
        int notLiveCount = 0;
        for (YTTile tile : tiles) {
            if (!tile.isLive()) {
                notLiveCount++;
                ConsoleLog.info(tile.getTitle() + " | duration: " + tile.getDuration());
            }
        }
        ConsoleLog.info("Not live", notLiveCount + " of " + tiles.size());
    }

    private void assertNoTileIsIncomplete(List<YTTile> tiles) {
        for (YTTile tile : tiles) {
            assertFalse(tile.getTitle() == null || tile.getTitle().isEmpty(),
                    "A collected tile has no title: " + tile);
            assertFalse(tile.getChannelName() == null || tile.getChannelName().isEmpty(),
                    "A collected tile has no channel name: " + tile);
            assertFalse(tile.getDuration() == null || tile.getDuration().isEmpty(),
                    "A collected tile has no duration: " + tile);
        }
    }
}
