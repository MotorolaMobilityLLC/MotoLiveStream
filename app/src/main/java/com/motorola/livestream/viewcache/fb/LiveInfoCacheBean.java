package com.motorola.livestream.viewcache.fb;

import com.motorola.livestream.model.fb.Comment;
import com.motorola.livestream.model.fb.Cursors;
import com.motorola.livestream.model.fb.LiveInfo;
import com.motorola.livestream.model.fb.LiveViews;
import com.motorola.livestream.model.fb.Reaction;
import com.motorola.livestream.model.fb.User;
import com.motorola.livestream.viewcache.ViewCacheBean;

import java.util.ArrayList;
import java.util.List;

public class LiveInfoCacheBean implements ViewCacheBean {

    private static final int MAX_COMMENTS_SIZE = 1000;

    private LiveInfo mLiveInfo;
    private User mUser;

    private List<Comment> mLiveComments = new ArrayList<>();
    private Cursors mLiveCommentCursor = null;
    private int nTotalComments;

    private LiveViews mLiveViews;

    private List<Reaction> mLiveReactions = new ArrayList<>();
    private Cursors mLiveReactionCursor = null;
    private int nTotalLikes;

    public void setLiveInfo(LiveInfo liveInfo) {
        mLiveInfo = null;
        mLiveInfo = liveInfo;
    }

    public String getLiveStreamUrl() {
        if (mLiveInfo == null) {
            return null;
        }

        return mLiveInfo.getSecureStreamUrl();
    }

    public String getLiveStreamId() {
        if (mLiveInfo == null) {
            return null;
        }

        return mLiveInfo.getId();
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }

    public List<Comment> getLiveComments() {
        // Make sure the comments are shown aligned to the bottom
        // so we need to add some empty comments to the head
        if (mLiveComments.size() < 4) {
            int emptyCommentCount = 4 - mLiveComments.size();
            List<Comment> newLiveComments = new ArrayList<>(mLiveComments);
            for (int i = 0; i < emptyCommentCount; i++) {
                newLiveComments.add(0, Comment.EMPTY);
            }
            return newLiveComments;
        }
        return mLiveComments;
    }

    public void addLiveComments(List<Comment> comments) {
        // Always add the new comment at last
        mLiveComments.addAll(comments);

        // Check the size, avoid over counting to the max size
        int overCount = mLiveComments.size() - MAX_COMMENTS_SIZE;
        if (overCount > 0) {
            // Remove the oldest comments from head
            for (int i = 0; i < overCount; i++) {
                mLiveComments.remove(0);
            }
        }
    }

    public Cursors getLiveCommentCursor() {
        return mLiveCommentCursor;
    }

    public void setLiveCommentCursor(Cursors cursor) {
        mLiveCommentCursor = cursor;
    }

    public int getTotalComments() {
        return nTotalComments;
    }

    public void setTotalComments(int count) {
        nTotalComments = count;
    }

    public void clearComments() {
        mLiveComments.clear();
        mLiveCommentCursor = null;
        nTotalComments = 0;
    }

    public LiveViews getLiveViews() {
        return mLiveViews;
    }

    public void setLiveViews(LiveViews liveviews) {
        mLiveViews = null;
        mLiveViews = liveviews;
    }

    public List<Reaction> getLiveReactions() {
        return mLiveReactions;
    }

    public void addLiveReactions(List<Reaction> reactions) {
        mLiveReactions.addAll(reactions);
    }

    public void clearLiveReactions() {
        mLiveReactions.clear();
    }

    public Cursors getLiveReactionCursor() {
        return mLiveReactionCursor;
    }

    public void setLiveReactionCursor(Cursors cursor) {
        mLiveReactionCursor = cursor;
    }

    public int getTotalLikes() {
        return nTotalLikes;
    }

    public void setTotalLikes(int count) {
        nTotalLikes += count;
    }

    public void clearReactions() {
        mLiveReactions.clear();
        mLiveReactionCursor = null;
        nTotalLikes = 0;
    }

    @Override
    public void clean() {
        mLiveInfo = null;
        mUser = null;

        clearComments();

        mLiveViews = null;

        clearReactions();
    }
}
