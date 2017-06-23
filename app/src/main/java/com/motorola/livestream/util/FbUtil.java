package com.motorola.livestream.util;

import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.motorola.livestream.model.fb.Comment;
import com.motorola.livestream.model.fb.Cursors;
import com.motorola.livestream.model.fb.FriendList;
import com.motorola.livestream.model.fb.LiveInfo;
import com.motorola.livestream.model.fb.LiveViews;
import com.motorola.livestream.model.fb.Reaction;
import com.motorola.livestream.model.fb.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FbUtil {

    public static final int PAGE_SIZE = 50;

    public interface OnListRetrievedListener<T> {
        void onSuccess(List<T> dataList);
        void onError(Exception exp);
    }

    public interface OnPagedListRetrievedListener<T> {
        void onSuccess(List<T> dataList, Cursors cursors, int totalCount);
        void onError(Exception exp);
    }

    public interface OnDataRetrievedListener<T> {
        void onSuccess(T data);
        void onError(Exception exp);
    }

    public static void getUserPhoto(OnDataRetrievedListener<User> listener, User user) {
        getUserPhoto(listener, user, true);
    }

    public static void getUserPhoto(OnDataRetrievedListener<User> listener, User user, boolean async) {
        Bundle parameters = new Bundle();
        parameters.putBoolean("redirect", false);

        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getUserPhotoPath(user.getId(), 160),
                parameters,
                HttpMethod.GET,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        try {
                            String userPhotoUrl = response.getJSONObject()
                                    .getJSONObject("data").getString("url");
                            user.setUserPhotoUrl(userPhotoUrl);
                            listener.onSuccess(user);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            user.setUserPhotoUrl(null);
                            listener.onError(e);
                        }
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        );

        if (async) {
            request.executeAsync();
        } else {
            request.executeAndWait();
        }
    }

    public static void getFriendList(OnListRetrievedListener<FriendList> listener, String userId) {
        getFriendList(listener, userId, true);
    }

    private static void getFriendList(OnListRetrievedListener<FriendList> listener, String userId,
                                      boolean onlyUserCreated) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name");
        if (onlyUserCreated) {
            parameters.putString("list_type", "user_created");
        }

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getFriendListsPath(userId),
                parameters,
                HttpMethod.GET,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        try {
                            List<FriendList> friendList = JSON.parseArray(
                                    response.getJSONObject().getJSONArray("data").toString(),
                                    FriendList.class);
                            listener.onSuccess(friendList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            listener.onError(e);
                        }
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void createUserLive(OnDataRetrievedListener<LiveInfo> listener, String userId,
                                      String description, String privacy, boolean isSpherical) {
        Bundle parameters = new Bundle();
        parameters.putString("description", description);
        parameters.putBoolean("save_vod", true);
        parameters.putBoolean("is_spherical", isSpherical);
        parameters.putString("privacy", privacy);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getUserLivePath(userId),
                parameters,
                HttpMethod.POST,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        LiveInfo liveInfo = JSON.parseObject(
                                response.getJSONObject().toString(), LiveInfo.class);
                        listener.onSuccess(liveInfo);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void getLiveViews(OnDataRetrievedListener<LiveViews> listener, String liveId) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "live_views,total_views");
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLivePath(liveId),
                parameters,
                HttpMethod.GET,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        LiveViews liveViews = JSON.parseObject(
                                response.getJSONObject().toString(),
                                LiveViews.class);
                        listener.onSuccess(liveViews);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void getLiveComments(OnPagedListRetrievedListener<Comment> listener,
                                       String liveId, int limit, Cursors cursors) {
        Bundle parameters = new Bundle();
        // Always get the total count
        parameters.putString("summary", "total_count");
        // Sort by publish date desc
        parameters.putString("order", "chronological");
        if (limit > 0) {
            parameters.putInt("limit", limit);
        }
        // At the live beginning, the comment list should only be one page
        // And we should refresh by the cursor returned last time returned for new comment
        if (cursors != null) {
            parameters.putString("after",  cursors.getAfter());
        }
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLiveCommentsPath(liveId),
                parameters,
                HttpMethod.GET,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        try {
                            JSONObject responseJson = response.getJSONObject();
                            List<Comment> commentList = JSON.parseArray(
                                    responseJson.getJSONArray("data").toString(),
                                    Comment.class);

                            Cursors newCursors = null;
                            if (responseJson.has("paging")) {
                                JSONObject pagingJson = responseJson.getJSONObject("paging");
                                if (pagingJson.has("cursors")) {
                                    newCursors = JSON.parseObject(
                                            pagingJson.getJSONObject("cursors").toString(),
                                            Cursors.class);
                                }
                            }

                            int totalCount = 0;
                            if (responseJson.has("summary")) {
                                JSONObject summaryJson = responseJson.getJSONObject("summary");
                                totalCount = summaryJson.getInt("total_count");
                            }
                            listener.onSuccess(commentList, newCursors, totalCount);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            listener.onError(e);
                        }
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void getLiveReactions(OnPagedListRetrievedListener<Reaction> listener,
                                        String liveId, int limit, Cursors cursors) {
        Bundle parameters = new Bundle();
        if (limit > 0) {
            parameters.putInt("limit", limit);
        }
        // we should refresh by the cursor returned last time returned for new reaction
        if (cursors != null) {
            parameters.putString("before",  cursors.getBefore());
        }
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLiveReactionsPath(liveId),
                parameters,
                HttpMethod.GET,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        try {
                            JSONObject responseJson = response.getJSONObject();
                            List<Reaction> reactionList = JSON.parseArray(
                                    responseJson.getJSONArray("data").toString(),
                                    Reaction.class);
                            int totalLikeCount = 0;
                            for (Reaction reaction : reactionList) {
                                if (Reaction.ReactionType.LIKE == reaction.getType()) {
                                    totalLikeCount++;
                                }
                            }

                            Cursors newCursors = null;
                            if (responseJson.has("paging")) {
                                JSONObject pagingJson = responseJson.getJSONObject("paging");
                                if (pagingJson.has("cursors")) {
                                    newCursors = JSON.parseObject(
                                            pagingJson.getJSONObject("cursors").toString(),
                                            Cursors.class);
                                }
                            }

                            listener.onSuccess(reactionList, newCursors, totalLikeCount);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            listener.onError(e);
                        }
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void stopLiveVideo(OnDataRetrievedListener<Boolean> listener, String liveId) {
        Bundle parameters = new Bundle();
        parameters.putBoolean("end_live_video", true);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLivePath(liveId),
                parameters,
                HttpMethod.POST,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        listener.onSuccess(true);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void updateLive(OnDataRetrievedListener<Boolean> listener, String liveId,
                                  String privacy) {
        Bundle parameters = new Bundle();
        parameters.putString("privacy", privacy);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLivePath(liveId),
                parameters,
                HttpMethod.POST,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        listener.onSuccess(true);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void deleteLive(OnDataRetrievedListener<Boolean> listener, String liveId) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getLivePath(liveId),
                null,
                HttpMethod.DELETE,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        listener.onSuccess(true);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }

    public static void deAuthorize(OnDataRetrievedListener<Boolean> listener) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                FbGraphPathUtil.getPermissionPath("me", null),
                null,
                HttpMethod.DELETE,
                (GraphResponse response) -> {
                    if (response.getError() == null) {
                        listener.onSuccess(true);
                    } else {
                        listener.onError(response.getError().getException());
                    }
                }
        ).executeAsync();
    }
}
