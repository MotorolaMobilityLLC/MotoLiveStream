package com.motorola.livestream.model.fb;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class LiveInfo {
    private String id;
    private String streamUrl;
    private String secureStreamUrl;
    private List<String> streamSecondaryUrls;
    private List<String> secureStreamSecondaryUrls;

    @JSONField(name="id")
    public String getId() {
        return id;
    }

    @JSONField(name="id")
    public void setId(String id) {
        this.id = id;
    }

    @JSONField(name="stream_url")
    public String getStreamUrl() {
        return streamUrl;
    }

    @JSONField(name="stream_url")
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    @JSONField(name="secure_stream_url")
    public String getSecureStreamUrl() {
        return secureStreamUrl;
    }

    @JSONField(name="secure_stream_url")
    public void setSecureStreamUrl(String secureStreamUrl) {
        this.secureStreamUrl = secureStreamUrl;
    }

    @JSONField(name="stream_secondary_urls")
    public List<String> getStreamSecondaryUrls() {
        return streamSecondaryUrls;
    }

    @JSONField(name="stream_secondary_urls")
    public void setStreamSecondaryUrls(List<String> streamSecondaryUrls) {
        this.streamSecondaryUrls = streamSecondaryUrls;
    }

    @JSONField(name="secure_stream_secondary_urls")
    public List<String> getSecureStreamSecondaryUrls() {
        return secureStreamSecondaryUrls;
    }

    @JSONField(name="secure_stream_secondary_urls")
    public void setSecureStreamSecondaryUrls(List<String> secureStreamSecondaryUrls) {
        this.secureStreamSecondaryUrls = secureStreamSecondaryUrls;
    }
}
