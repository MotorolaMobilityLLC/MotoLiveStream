package com.motorola.livestream.model.fb;

import com.google.gson.annotations.SerializedName;

public class LiveViews {

    @SerializedName("live_views")
    private int liveViews;

    @SerializedName ("total_views")
    private int totalViews;

    public LiveViews() { }

    public int getLiveViews() {
        return liveViews;
    }

    public void setLiveViews(int liveViews) {
        this.liveViews = liveViews;
    }

    public int getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }
}
