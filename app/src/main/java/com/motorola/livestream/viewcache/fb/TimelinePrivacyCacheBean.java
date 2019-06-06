package com.motorola.livestream.viewcache.fb;

import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.TimelinePrivacy;
import com.motorola.livestream.viewcache.ViewCacheBean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TimelinePrivacyCacheBean implements ViewCacheBean {

    private static final int[] PRIVACY_TITLE = {
            R.string.privacy_world,
            R.string.privacy_friends,
            R.string.privacy_self
    };

    private static final int[] PRIVACY_DESCRIPTION = {
            R.string.privacy_world_description,
            R.string.privacy_friends_description,
            R.string.privacy_self_description
    };

    private static final int[] PRIVACY_ICON = {
            R.drawable.privacy_scope_everyone_gray,
            R.drawable.privacy_scope_friends_gray,
            R.drawable.privacy_scope_only_me_gray
    };

    private static final int[] PRIVACY_ICON_WHITE = {
            R.drawable.privacy_scope_everyone_white,
            R.drawable.privacy_scope_friends_white,
            R.drawable.privacy_scope_only_me_white
    };

    // CUSTOM should ahead of SELF
    private static final int[] PRIVACY_INDEX = {
            0,
            1,
            3,
            2
    };

    private static final String[] PRIVACY_VALUE = {
            "EVERYONE",
            "ALL_FRIENDS",
            "SELF",
            "CUSTOM"
    };

    private static final String EMPTY_VALUE = "";

    private TimelinePrivacy mPrivacy = TimelinePrivacy.FRIENDS;

    public TimelinePrivacy getPrivacy() {
        return mPrivacy;
    }

    public void setPrivacy(TimelinePrivacy privacy) {
        mPrivacy = privacy;
    }

    public int getCurrentPrivacyIndex() {
        return PRIVACY_INDEX[mPrivacy.ordinal()];
    }

    public int getPrivacyTitle() {
        return PRIVACY_TITLE[mPrivacy.ordinal()];
    }

    public int getPrivacyDescription() {
        return PRIVACY_DESCRIPTION[mPrivacy.ordinal()];
    }

    public int getPrivacyIcon(boolean isForList) {
        return isForList ? PRIVACY_ICON[mPrivacy.ordinal()]
                : PRIVACY_ICON_WHITE[mPrivacy.ordinal()];
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("value", PRIVACY_VALUE[mPrivacy.ordinal()]);
            json.put("friends", EMPTY_VALUE);
            json.put("allow", EMPTY_VALUE);
            json.put("deny", EMPTY_VALUE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    public void clean() {
        mPrivacy = TimelinePrivacy.FRIENDS;
    }

    public static int getPrivacyTitle(TimelinePrivacy privacyValue) {
        return PRIVACY_TITLE[privacyValue.ordinal()];
    }

    public static int getPrivacyDescription(TimelinePrivacy privacyValue) {
        return PRIVACY_DESCRIPTION[privacyValue.ordinal()];
    }

    public static int getPrivacyIcon(TimelinePrivacy privacyValue) {
        return PRIVACY_ICON[privacyValue.ordinal()];
    }
}
