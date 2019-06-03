package com.motorola.livestream.model.fb;

import androidx.collection.LruCache;

public class User {
    private String id;
    private String name;
    private String url;

    private static final LruCache<String, String> sUserPhotoCache =
            new LruCache<String, String>(1024) {
                @Override
                protected int sizeOf(String key, String value) {
                    return 1;
                }
            };

    public User() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserPhotoUrl() {
        return url;
    }

    public void setUserPhotoUrl(String url) {
        this.url = url;
        if (url != null) {
            cacheUserPhoto(id, url);
        }
    }

    private static void cacheUserPhoto(String userId, String userPhotoUrl) {
        synchronized (sUserPhotoCache) {
            sUserPhotoCache.put(userId, userPhotoUrl);
        }
    }

    public static String getCachedUserPhoto(String userId) {
        synchronized (sUserPhotoCache) {
            return sUserPhotoCache.get(userId);
        }
    }
}
