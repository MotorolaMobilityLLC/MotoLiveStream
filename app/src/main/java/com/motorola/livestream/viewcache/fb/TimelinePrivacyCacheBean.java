package com.motorola.livestream.viewcache.fb;

import com.alibaba.fastjson.JSONObject;
import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.TimelinePrivacy;
import com.motorola.livestream.viewcache.ViewCacheBean;

import java.util.ArrayList;
import java.util.List;

public class TimelinePrivacyCacheBean implements ViewCacheBean {

    private static final int[] PRIVACY_TITLE = {
            R.string.privacy_world,
            R.string.privacy_friends,
            R.string.privacy_self,
            R.string.privacy_custom,
    };

    private static final int[] PRIVACY_DESCRIPTION = {
            R.string.privacy_world_description,
            R.string.privacy_friends_description,
            R.string.privacy_self_description,
            R.string.privacy_custom_description,
    };

    private static final int[] PRIVACY_ICON = {
            R.drawable.privacy_scope_everyone_gray,
            R.drawable.privacy_scope_friends_gray,
            R.drawable.privacy_scope_only_me_gray,
            R.drawable.privacy_scope_list_gray,
    };

    private static final int[] PRIVACY_ICON_WHITE = {
            R.drawable.privacy_scope_everyone_white,
            R.drawable.privacy_scope_friends_white,
            R.drawable.privacy_scope_only_me_white,
            R.drawable.privacy_scope_list_white,
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

    private static final String CUSTOM_FRIENDS_VALUE = "SOME_FRIENDS";
    private static final String EMPTY_VALUE = "";

    private TimelinePrivacy mPrivacy = TimelinePrivacy.FRIENDS;
    private List<String> mCustomFriendlist = new ArrayList<>();
    private String mCustomFriendlistDisplay = null;

    public TimelinePrivacy getPrivacy() {
        return mPrivacy;
    }

    public void setPrivacy(TimelinePrivacy privacy) {
        mPrivacy = privacy;
    }

    public int getCurrentPrivacyIndex() {
        return PRIVACY_INDEX[mPrivacy.ordinal()];
    }

    public int getProvacyTitle() {
        return PRIVACY_TITLE[mPrivacy.ordinal()];
    }

    public int getProvacyDescription() {
        return PRIVACY_DESCRIPTION[mPrivacy.ordinal()];
    }

    public int getProvacyIcon(boolean isForList) {
        return isForList ? PRIVACY_ICON[mPrivacy.ordinal()]
                : PRIVACY_ICON_WHITE[mPrivacy.ordinal()];
    }

    public List<String> getPrivacyCustomFriendlist() {
        return mCustomFriendlist;
    }

    public void setPrivacyCustomFriendlist(List<String> newList) {
        mCustomFriendlist.clear();
        mCustomFriendlist.addAll(newList);
    }

    public String getPrivacyCustomFriendlistDisplay() {
        return mCustomFriendlistDisplay;
    }

    public void setPrivacyCustomFriendlistDisplay(String newValue) {
        mCustomFriendlistDisplay = newValue;
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        json.put("value", PRIVACY_VALUE[mPrivacy.ordinal()]);
        if (TimelinePrivacy.CUSTOM == mPrivacy) {
            json.put("friends", CUSTOM_FRIENDS_VALUE);
            json.put("allow", getCustomFriendlist());
            json.put("deny", EMPTY_VALUE);
        } else {
            json.put("friends", EMPTY_VALUE);
            json.put("allow", EMPTY_VALUE);
            json.put("deny", EMPTY_VALUE);
        }
        return json.toString();
    }

    @Override
    public void clean() {
        mPrivacy = TimelinePrivacy.FRIENDS;
        mCustomFriendlist.clear();
        mCustomFriendlistDisplay = null;
    }

    @Override
    public void save(String tag) {

    }

    public static int getProvacyTitle(TimelinePrivacy privacyValue) {
        return PRIVACY_TITLE[privacyValue.ordinal()];
    }

    public static int getProvacyDescription(TimelinePrivacy privacyValue) {
        return PRIVACY_DESCRIPTION[privacyValue.ordinal()];
    }

    public static int getProvacyIcon(TimelinePrivacy privacyValue) {
        return PRIVACY_ICON[privacyValue.ordinal()];
    }

    private String getCustomFriendlist() {
        StringBuilder sb = new StringBuilder();
        for (String friendlistId : mCustomFriendlist) {
            sb.append(friendlistId).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
