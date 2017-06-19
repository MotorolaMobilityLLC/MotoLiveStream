package com.motorola.livestream.viewcache;

import android.text.TextUtils;

import com.motorola.livestream.viewcache.fb.LiveInfoCacheBean;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import java.util.HashMap;

public class ViewCacheManager {
    public static final String FB = "1";
    public static final String FB_TIMELINE_PRIVACY = "11";
    public static final String FB_LIVE_INFO = "12";

    private static HashMap<String, String> sViewCacheClassMap;
    private static HashMap<String, HashMap<String, ViewCacheBean>> sViewCaches;

    public static ViewCacheBean getCacheFromTag(String tagName) {
        if (sViewCacheClassMap == null) {
            initCacheMap();
        }

        if (TextUtils.isEmpty(tagName)) {
            return null;
        } else if (tagName.startsWith(FB)) {
            return getFBCache(tagName);
        } else {
            return null;
        }
    }

    private static ViewCacheBean getFBCache(String cacheName) {
        if (sViewCaches == null) {
            sViewCaches = new HashMap<>();
        }

        HashMap<String, ViewCacheBean> fbCacheMap = sViewCaches.get(FB);
        if (fbCacheMap == null) {
            fbCacheMap = new HashMap<>();
            sViewCaches.put(FB, fbCacheMap);
        }

        ViewCacheBean viewCacheBean = fbCacheMap.get(cacheName);
        if (viewCacheBean == null) {
            viewCacheBean = getCacheBean(cacheName);
            fbCacheMap.put(cacheName, viewCacheBean);
        }

        return viewCacheBean;
    }

    private static ViewCacheBean getCacheBean(String cacheName) {
        String beanClassName = sViewCacheClassMap.get(cacheName);
        try {
            return (ViewCacheBean) Class.forName(beanClassName).newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void initCacheMap() {
        HashMap<String, String> tempMap = new HashMap<>();

        // Facebook related view cache
        tempMap.put(FB_TIMELINE_PRIVACY, TimelinePrivacyCacheBean.class.getName());
        tempMap.put(FB_LIVE_INFO, LiveInfoCacheBean.class.getName());

        sViewCacheClassMap = tempMap;
    }
}
