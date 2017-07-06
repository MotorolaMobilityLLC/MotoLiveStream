package com.motorola.livestream.model.fb;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LiveInfo {

    @SerializedName("id")
    private String id;

    @SerializedName("stream_url")
    private String streamUrl;

    @SerializedName("secure_stream_url")
    private String secureStreamUrl;

    @SerializedName("stream_secondary_urls")
    private List<String> streamSecondaryUrls;

    @SerializedName("secure_stream_secondary_urls")
    private List<String> secureStreamSecondaryUrls;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getSecureStreamUrl() {
        return secureStreamUrl;
    }

    public void setSecureStreamUrl(String secureStreamUrl) {
        this.secureStreamUrl = secureStreamUrl;
    }

    public List<String> getStreamSecondaryUrls() {
        return streamSecondaryUrls;
    }

    public void setStreamSecondaryUrls(List<String> streamSecondaryUrls) {
        this.streamSecondaryUrls = streamSecondaryUrls;
    }

    public List<String> getSecureStreamSecondaryUrls() {
        return secureStreamSecondaryUrls;
    }

    public void setSecureStreamSecondaryUrls(List<String> secureStreamSecondaryUrls) {
        this.secureStreamSecondaryUrls = secureStreamSecondaryUrls;
    }
}
