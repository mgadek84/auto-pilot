package com.gitlab.rmarzec.model;

/**
 * Data of a single YouTube search result tile.
 *
 * <p>Live streams have no fixed length, so {@link #LIVE_DURATION} is stored as their
 * duration.
 */
public class YTTile {

    public static final String LIVE_DURATION = "live";

    private String title;
    private String channelName;
    private String duration;

    public YTTile() {
    }

    public YTTile(String title, String channelName, String duration) {
        this.title = title;
        this.channelName = channelName;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isLive() {
        return LIVE_DURATION.equalsIgnoreCase(duration);
    }

    @Override
    public String toString() {
        return "YTTile{title='" + title + "', channelName='" + channelName + "', duration='" + duration + "'}";
    }
}
