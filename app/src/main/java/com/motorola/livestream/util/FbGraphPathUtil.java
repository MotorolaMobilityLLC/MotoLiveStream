package com.motorola.livestream.util;

import android.text.TextUtils;

public class FbGraphPathUtil {

    private static final String USER_PHOTO_PATH = "/%1s/picture?width=%2d&height=%3d";
    private static final String FRIEND_LIST_PATH = "/%s/friendlists";
    private static final String USER_LIVE_PATH = "/%s/live_videos";
    private static final String LIVE_PATH = "/%s";
    private static final String LIVE_COMMENTS_PATH = "/%s/comments";
    private static final String LIVE_REACTIONS_PATH = "/%s/reactions";
    private static final String PERMISSIONS_PATH = "/%1s/permissions";
    private static final String PERMISSION_PATH = "/%1s/permissions/%2s";

    public static String getUserPhotoPath(String userId, int size) {
        return String.format(USER_PHOTO_PATH, userId, size, size);
    }

    public static String getFriendListsPath(String userId) {
        return String.format(FRIEND_LIST_PATH, userId);
    }

    public static String getUserLivePath(String userId) {
        return String.format(USER_LIVE_PATH, userId);
    }

    public static String getLivePath(String liveId) {
        return String.format(LIVE_PATH, liveId);
    }

    public static String getLiveCommentsPath(String liveId) {
        return String.format(LIVE_COMMENTS_PATH, liveId);
    }

    public static String getLiveReactionsPath(String liveId) {
        return String.format(LIVE_REACTIONS_PATH, liveId);
    }

    public static String getPermissionPath(String userId, String permission) {
        if (TextUtils.isEmpty(permission)) {
            return String.format(PERMISSIONS_PATH, userId);
        } else {
            return String.format(PERMISSION_PATH, userId, permission);
        }
    }

}
