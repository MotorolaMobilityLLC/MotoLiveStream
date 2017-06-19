package com.motorola.livestream.model.fb;

import com.alibaba.fastjson.annotation.JSONField;

public class LiveViews {

    private int liveViews;
    private int totalViews;

    public LiveViews() { }

    @JSONField (name="live_views")
    public int getLiveViews() {
        return liveViews;
    }

    @JSONField (name="live_views")
    public void setLiveViews(int liveViews) {
        this.liveViews = liveViews;
    }

    @JSONField (name="total_views")
    public int getTotalViews() {
        return totalViews;
    }

    @JSONField (name="total_views")
    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }
}
