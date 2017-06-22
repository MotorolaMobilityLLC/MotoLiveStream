package com.motorola.livestream.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.github.faucamp.simplertmp.RtmpHandler;
import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.Comment;
import com.motorola.livestream.model.fb.Cursors;
import com.motorola.livestream.model.fb.LiveInfo;
import com.motorola.livestream.model.fb.LiveViews;
import com.motorola.livestream.model.fb.Reaction;
import com.motorola.livestream.model.fb.User;
import com.motorola.livestream.ui.adapter.CommentListAdapter;
import com.motorola.livestream.ui.animate.ReactionView;
import com.motorola.livestream.ui.privacy.TimelineActivity;
import com.motorola.livestream.util.FbUtil;
import com.motorola.livestream.util.FbUtil.OnDataRetrievedListener;
import com.motorola.livestream.util.FbUtil.OnPagedListRetrievedListener;
import com.motorola.livestream.ui.widget.LiveCountingTimer;
import com.motorola.livestream.util.Log;
import com.motorola.livestream.util.SettingsPref;
import com.motorola.livestream.util.Util;
import com.motorola.livestream.viewcache.ViewCacheManager;
import com.motorola.livestream.viewcache.fb.LiveInfoCacheBean;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;

public class LiveMainFragment extends Fragment
        implements SrsEncodeHandler.SrsEncodeListener, RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, View.OnClickListener {

    private static final String LOG_TAG = "LiveMainFragment";

    private static final int REQUEST_LIVE_PRIVACY = 0x199;

    private static final int LIVE_INFO_REFRESH_INTERVAL = 3000; //3sec

    private static final int MSG_START_LIVE = 0x101;
    private static final int MSG_UPDATE_LIVE_COMMENTS = 0x102;
    private static final int MSG_UPDATE_LIVE_VIEWS = 0x103;
    private static final int MSG_UPDATE_LIVE_REACTIONS = 0x104;
    private static final int MSG_LIVE_STOPPED = 0x105;

    public static Fragment newInstance() {
        return new LiveMainFragment();
    }

    private SrsPublisher mPublisher = null;

    private RequestOptions mRequestOptions;

    private View mLoadingLayout;

    private View mTopLayout;
    private LiveCountingTimer mLiveTimer;

    private View mLiveSettings;
    private ImageView mUserAvatar;
    private TextView mUserName;
    private ImageView mPrivacyIcon;
    private TextView mPrivacyTitle;
    private EditText mLiveInfoInput;

    private View mGoLiveLayout;
    private ImageButton mBtnGoLive;
    private TextView mGoLiveLabel;

    private View mLiveInteract;
    private TextView mLiveComments;
    private TextView mLiveViews;

    private View mResultLayout;
    private Button mBtnPostLive;
    private Button mBtnDelLive;
    private TextView mLiveResultPrivacy;

    private View mCommentLayout;
    private ReactionView mReactionView;
    private RecyclerView mCommentListView;
    private CommentListAdapter mCommentAdapter;

    private TimelinePrivacyCacheBean mPrivacyCacheBean = null;
    private LiveInfoCacheBean mLiveInfoCacheBean = null;

    private boolean mIsOnLive = false;

    private Timer mLiveCommentsTimer;
    private OnPagedListRetrievedListener<Comment> mLiveCommentListener =
            new OnPagedListRetrievedListener<Comment>() {
                @Override
                public void onSuccess(List<Comment> dataList, Cursors cursors, int totalCount) {
                    if (dataList.size() > 0) {
                        mLiveInfoCacheBean.addLiveComments(dataList);
                        mLiveInfoCacheBean.setLiveCommentCursor(cursors);
                        if (totalCount > 0) {
                            mLiveInfoCacheBean.setTotalComments(totalCount);
                        }

                        mHandler.sendEmptyMessage(MSG_UPDATE_LIVE_COMMENTS);

                        if (dataList.size() == FbUtil.PAGE_SIZE) {
                            // Maybe the list is not reach to end
                            // so we need to retrieve next page more quicker.
                            mLiveCommentsTimer.schedule(new TimerTask() {
                                public void run() {
                                    LiveMainFragment.this.startGetComment();
                                }
                            }, 800);
                            return;
                        }
                    }
                    // Wait 3 seconds to check new comments
                    LiveMainFragment.this.startToGetComment();
                }

                @Override
                public void onError(Exception exp) {
                    Log.e(LOG_TAG, "get live comments failed, wait to re-get");
                    exp.printStackTrace();
                    LiveMainFragment.this.startToGetComment();
                }
            };

    private Timer mLiveViewsTimer;
    private OnDataRetrievedListener<LiveViews> mLiveViewsListener =
            new OnDataRetrievedListener<LiveViews>() {
                @Override
                public void onSuccess(LiveViews data) {
                    mLiveInfoCacheBean.setLiveViews(data);
                    mHandler.sendEmptyMessage(MSG_UPDATE_LIVE_VIEWS);
                    LiveMainFragment.this.startToGetViews();
                }

                @Override
                public void onError(Exception exp) {
                    Log.e(LOG_TAG, "get live views failed, wait to re-get");
                    exp.printStackTrace();
                    LiveMainFragment.this.startToGetViews();
                }
            };

    private Timer mLiveReactionsTimer;
    private OnPagedListRetrievedListener<Reaction> mLiveReactionsListener =
            new OnPagedListRetrievedListener<Reaction>() {
                @Override
                public void onSuccess(List<Reaction> dataList, Cursors cursors, int totalCount) {
                    if (dataList.size() > 0) {
                        mLiveInfoCacheBean.addLiveReactions(dataList);
                        mLiveInfoCacheBean.setLiveReactionCursor(cursors);
                        mLiveInfoCacheBean.setTotalLikes(totalCount);
                        // Delay 500 millis seconds, make sure comment list could be refreshed first
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_LIVE_REACTIONS, 500L);
                    }
                    LiveMainFragment.this.startToGetReaction();
                }

                @Override
                public void onError(Exception exp) {
                    Log.e(LOG_TAG, "get live reactions failed, wait to re-get");
                    exp.printStackTrace();
                    startToGetReaction();
                }
            };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_LIVE:
                    onLiveStart();
                    break;
                case MSG_UPDATE_LIVE_COMMENTS:
                    updateLiveComments();
                    break;
                case MSG_UPDATE_LIVE_VIEWS:
                    updateLiveViewers();
                    break;
                case MSG_UPDATE_LIVE_REACTIONS:
                    updateLiveReactions();
                    break;
                case MSG_LIVE_STOPPED:
                    mLoadingLayout.setVisibility(View.GONE);
                    showLiveStreamResult();
                    break;
            }
        }
    };

    private DialogInterface.OnClickListener mResumeDialogListener =
            (DialogInterface dialog, int which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mPublisher.startCamera();
                        mPublisher.setSendVideoOnly(false);

                        mLiveTimer.resumeCounting();
                        startUpdateInteractInfo();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        stopLive();
                        mBtnGoLive.setSelected(false);
                        break;
                }
            };

    private AlertDialog mPostDialog = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mPrivacyCacheBean == null) {
            mPrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_TIMELINE_PRIVACY);
        }
        if (mLiveInfoCacheBean == null) {
            mLiveInfoCacheBean = (LiveInfoCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_LIVE_INFO);
        }

        if (Profile.getCurrentProfile() == null) {
            new ProfileTracker() {
                @Override
                protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                    stopTracking();
                    updateUserInfo(currentProfile);
                }
            }.startTracking();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWidgets(view);

        mPublisher = new SrsPublisher((SrsCameraView) view.findViewById(R.id.live_camera_view));
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        mPublisher.setPreviewResolution(720, 1280);
        mPublisher.setOutputResolution(720, 1280);
        mPublisher.setVideoHDMode();
        mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsOnLive) {
            //Popup a dialog to indicate user whether to resume or stop the live
            showResumeDialog();
        } else {
            mPublisher.startCamera();

            updateUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLiveTimer != null) {
            mLiveTimer.pauseCounting();
        }
        if (mReactionView != null) {
            mReactionView.stop();
        }
        if (mPublisher != null) {
            mPublisher.setSendVideoOnly(true);
            mPublisher.stopCamera();
        }
        stopUpdateInteractInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPublisher != null) {
            mPublisher.stopRecord();
            mPublisher.stopCamera();
        }

        mPrivacyCacheBean.clean();
        mLiveInfoCacheBean.clean();

        mLiveTimer.stopCounting();
        mIsOnLive = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LIVE_PRIVACY) {
            if (resultCode == RESULT_OK) {
                updateLivePrivacySettings();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initWidgets(View view) {
        mLoadingLayout = view.findViewById(R.id.create_live_loading);

        mTopLayout = view.findViewById(R.id.layout_top);
        mLiveTimer = (LiveCountingTimer) mTopLayout.findViewById(R.id.live_timer_view);
        mTopLayout.findViewById(R.id.btn_record_mute).setOnClickListener(this);

        mLiveSettings = view.findViewById(R.id.layout_live_settings);
        // User infos
        View userInfoLayout = mLiveSettings.findViewById(R.id.layout_user_info);
        userInfoLayout.setOnClickListener(this);
        mUserAvatar = (ImageView) userInfoLayout.findViewById(R.id.user_avatar);
        mUserName = (TextView) userInfoLayout.findViewById(R.id.user_name);

        // Privacy settings
        View privacyLayout = mLiveSettings.findViewById(R.id.layout_privacy_setting);
        mPrivacyIcon = (ImageView) privacyLayout.findViewById(R.id.privacy_icon);
        mPrivacyTitle = (TextView) privacyLayout.findViewById(R.id.privacy_title);
        privacyLayout.setOnClickListener(this);

        // Live description input
        mLiveInfoInput = (EditText) mLiveSettings.findViewById(R.id.live_description_input);

        mGoLiveLayout = view.findViewById(R.id.layout_go_live);
        mBtnGoLive = (ImageButton) mGoLiveLayout.findViewById(R.id.btn_go_live);
        mBtnGoLive.setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_capture).setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_switch_camera).setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_select_camera).setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_camera_mode).setOnClickListener(this);
        mGoLiveLabel = (TextView) mGoLiveLayout.findViewById(R.id.label_golive);
        mGoLiveLabel.setVisibility(View.VISIBLE);

        mLiveInteract = view.findViewById(R.id.layout_live_interact);
        mLiveComments = (TextView) mLiveInteract.findViewById(R.id.live_comments_view);
        mLiveComments.setOnClickListener(this);
        mLiveViews = (TextView) mLiveInteract.findViewById(R.id.live_views_view);

        mResultLayout = view.findViewById(R.id.layout_live_result);
        mBtnPostLive = (Button) mResultLayout.findViewById(R.id.btn_post_live);
        mBtnPostLive.setOnClickListener(this);
        mBtnDelLive = (Button) mResultLayout.findViewById(R.id.btn_delete_live);
        mBtnDelLive.setOnClickListener(this);
        mLiveResultPrivacy = (TextView) mResultLayout.findViewById(R.id.privacy_view);
        mLiveResultPrivacy.setOnClickListener(this);

        mCommentLayout = view.findViewById(R.id.layout_live_comments);
        mReactionView = (ReactionView) mCommentLayout.findViewById(R.id.reaction_view);
        mCommentListView = (RecyclerView) mCommentLayout.findViewById(R.id.comment_list);
        mCommentListView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mCommentAdapter = new CommentListAdapter(mCommentListView);
        mCommentListView.setAdapter(mCommentAdapter);
    }

    private void updateUI() {
        updateUserInfo(Profile.getCurrentProfile());
        updateLivePrivacySettings();
    }

    private void updateUserInfo(Profile profile) {
        if (profile == null) {
            // Wait ProfileTracker to update the current profile
            return;
        }
        if (mLiveInfoCacheBean == null) {
            mLiveInfoCacheBean = (LiveInfoCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_LIVE_INFO);
        }

        User currentUser = mLiveInfoCacheBean.getUser();
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setId(profile.getId());
            currentUser.setName(profile.getName());
            mLiveInfoCacheBean.setUser(currentUser);
        }

        if (getActivity() != null) {
            mUserName.setText(currentUser.getName());
            updateUserPhoto(currentUser);
        }
    }

    private void updateUserPhoto(User currentUser) {
        if (getActivity() == null) {
            return;
        }

        if (mRequestOptions == null) {
            mRequestOptions = new RequestOptions();
            mRequestOptions.placeholder(R.drawable.ic_user_photo_default)
                .circleCrop();
        }
        if (TextUtils.isEmpty(currentUser.getUserPhotoUrl())) {
            String cachedUrl = SettingsPref.getUserPhotoUrl(getActivity());
            if (!TextUtils.isEmpty(cachedUrl)) {
                Glide.with(getActivity())
                        .load(cachedUrl)
                        .apply(mRequestOptions)
                        .into(mUserAvatar);
            }

            FbUtil.getUserPhoto(
                    new FbUtil.OnDataRetrievedListener<User>() {
                        @Override
                        public void onSuccess(User data) {
                            updateUserPhoto(data);
                        }

                        @Override
                        public void onError(Exception exp) {
                            Log.w(LOG_TAG, "Get user photo failed");
                            exp.printStackTrace();
                        }
                    }, currentUser);
        } else {
            String newUrl = currentUser.getUserPhotoUrl();
            Log.d(LOG_TAG, newUrl);

            String cachedUrl = SettingsPref.getUserPhotoUrl(getActivity());
            if (cachedUrl == null
                    || !cachedUrl.equals(newUrl)) {
                SettingsPref.saveUserPhotoUrl(getActivity(), newUrl);
            }
            Glide.with(getActivity())
                    .load(newUrl)
                    .apply(mRequestOptions)
                    .into(mUserAvatar);
        }
    }

    private void updateLivePrivacySettings() {
        if (mPrivacyCacheBean == null) {
            mPrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_TIMELINE_PRIVACY);
        }

        mPrivacyIcon.setImageResource(mPrivacyCacheBean.getProvacyIcon(false));
        switch (mPrivacyCacheBean.getPrivacy()) {
            case CUSTOM:
                mPrivacyTitle.setText(mPrivacyCacheBean.getPrivacyCustomFriendlistDisplay());
                mLiveResultPrivacy.setText(mPrivacyCacheBean.getPrivacyCustomFriendlistDisplay());
                break;
            default:
                mPrivacyTitle.setText(mPrivacyCacheBean.getProvacyTitle());
                mLiveResultPrivacy.setText(mPrivacyCacheBean.getProvacyTitle());
        }
    }

    private void handleException(Exception e) {
        try {
            mPublisher.stopPublish();
            mPublisher.stopRecord();
        } catch (Exception e1) {
        }
    }

    private void showLogoutDialog() {
        final FragmentActivity activity = getActivity();

        SpannableStringBuilder ssb =
                new SpannableStringBuilder(activity.getString(R.string.live_dlg_logout_message));
        ssb.append(mLiveInfoCacheBean.getUser().getName(),
                new StyleSpan(Typeface.BOLD), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        new AlertDialog.Builder(activity)
                .setMessage(ssb)
                .setPositiveButton(R.string.live_dlg_btn_logout,
                        (DialogInterface dialog, int which) -> {
                            logoutFromFacebook(false);
                        })
                .show();
    }

    private void showResumeDialog() {
        if (getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.live_popup_dlg_title)
                .setMessage(R.string.live_popup_dlg_content)
                .setNegativeButton(R.string.live_popup_dlg_btn_finish,
                        mResumeDialogListener)
                .setPositiveButton(R.string.live_popup_dlg_btn_resume,
                        mResumeDialogListener)
                .setCancelable(false)
                .show();
    }

    private void showLiveDeletedDialog() {
        if (getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.live_deleted_message)
                .setPositiveButton(R.string.live_process_identify,
                        (DialogInterface dialog, int which) -> {
                            mResultLayout.setVisibility(View.GONE);
                            refreshPreGoLiveUI();
                        })
                .setCancelable(false)
                .show();
    }

    private void showLivePostedDialog() {
        if (getActivity() == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.custom_live_dialog, null);
        Button btnPositive = (Button) view.findViewById(R.id.btn_positive);
        if (btnPositive != null) {
            btnPositive.setText(R.string.live_posted_view);
            btnPositive.setOnClickListener(this);
        }
        Button btnNegative = (Button) view.findViewById(R.id.btn_negative);
        if (btnNegative != null) {
            btnNegative.setText(R.string.live_process_identify);
            btnNegative.setOnClickListener(this);
        }

        builder.setView(view)
                .setMessage(R.string.live_posted_message)
                .setCancelable(false);

        mPostDialog = builder.show();
    }

    private void handleLivePostedDialogOnClick(int id) {
        if (mPostDialog != null) {
            mPostDialog.dismiss();
            mPostDialog = null;
        }
        mResultLayout.setVisibility(View.GONE);
        refreshPreGoLiveUI();

        if (R.id.btn_positive == id) {
            Util.jumpToFacebook(getActivity().getApplicationContext(),
                    mLiveInfoCacheBean.getUser());
        }
    }

    public void startGoLive() {
//        if (!((LiveDynamicActivity) getActivity()).checkIfNeedToRequestPermission()) {
//            // The checkIfNeedToRequestPermission function will handle the login procedure
//            // So just return here
//            return;
//        }

        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLiveInfoInput.getWindowToken(), 0);

        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.createUserLive(
                new FbUtil.OnDataRetrievedListener<LiveInfo>() {
                    @Override
                    public void onSuccess(LiveInfo liveInfo) {
                        Log.d(LOG_TAG, "Create live video ok.");
                        onLiveStreamReady(liveInfo);
                    }

                    @Override
                    public void onError(Exception exp) {
                        Log.e(LOG_TAG, "Create live video failed!");
                        exp.printStackTrace();

                        mLoadingLayout.setVisibility(View.GONE);
                    }
                },
                mLiveInfoCacheBean.getUser().getId(), mLiveInfoInput.getText().toString(),
                mPrivacyCacheBean.toJsonString(), false);
    }

    private void onLiveStreamReady(LiveInfo liveInfo) {
        if (mLiveInfoCacheBean == null) {
            mLiveInfoCacheBean = (LiveInfoCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_LIVE_INFO);
        }
        mLiveInfoCacheBean.setLiveInfo(liveInfo);
        Log.d(LOG_TAG, "onLiveStreamReady: " + mLiveInfoCacheBean.getLiveStreamUrl());

        if (mCommentAdapter != null) {
            mCommentAdapter.clearData();
            mCommentAdapter.notifyDataSetChanged();
        }
        if (mLiveCommentsTimer == null) {
            mLiveCommentsTimer = new Timer();
        }
        if (mLiveViewsTimer == null) {
            mLiveViewsTimer = new Timer();
        }
        if (mLiveReactionsTimer == null) {
            mLiveReactionsTimer = new Timer();
        }

        mLoadingLayout.setVisibility(View.GONE);

        mHandler.sendEmptyMessage(MSG_START_LIVE);
        // TODO maybe need a countdown timer to indicate starting live
//        new CountDownTimer(3100, 1000) {
//
//            @Override
//            public void onTick(long millisUntilFinished) {
//                Log.d(LOG_TAG, "onTick: " + millisUntilFinished);
//            }
//
//            @Override
//            public void onFinish() {
//                mHandler.sendEmptyMessage(MSG_START_LIVE);
//            }
//        }.start();
    }

    private void onLiveStart() {
        mTopLayout.setVisibility(View.VISIBLE);
        mLiveSettings.setVisibility(View.GONE);
        mCommentLayout.setVisibility(View.VISIBLE);

        // Clear the input text and remove focus
        mLiveInfoInput.setText(null);
        mLiveInfoInput.clearFocus();

        mLiveInteract.setVisibility(View.VISIBLE);
        mBtnGoLive.setSelected(true);
        mGoLiveLabel.setVisibility(View.GONE);

        mPublisher.startPublish(mLiveInfoCacheBean.getLiveStreamUrl());
        mPublisher.startCamera();

        mLiveTimer.startCounting();
        startUpdateInteractInfo();
        mIsOnLive = true;
    }

    private void startUpdateInteractInfo() {
        startGetComment();
        startGetViews();
        startGetReaction();
    }

    private void stopUpdateInteractInfo() {
        stopGetComment();
        stopGetViews();
        stopGetReaction();
    }

    private void stopLive() {
        mIsOnLive = false;
        mLoadingLayout.setVisibility(View.VISIBLE);
        stopUpdateInteractInfo();
        mLiveTimer.stopCounting();

        FbUtil.stopLiveVideo(
                new FbUtil.OnDataRetrievedListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        mHandler.sendEmptyMessage(MSG_LIVE_STOPPED);
                    }

                    @Override
                    public void onError(Exception exp) {
                        exp.printStackTrace();

                        mHandler.sendEmptyMessage(MSG_LIVE_STOPPED);
                    }
                },
                mLiveInfoCacheBean.getLiveStreamId());

        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    private void showLiveStreamResult() {
        // Hide go live controller layout
        mTopLayout.setVisibility(View.GONE);
        mGoLiveLayout.setVisibility(View.GONE);
        mBtnGoLive.setSelected(false);
        mLiveInteract.setVisibility(View.GONE);

        // Stop reaction animation and clear reactions
        mReactionView.stop();
        mReactionView.clear();
        mCommentLayout.setVisibility(View.GONE);

        // Show live result dialog
        mResultLayout.setVisibility(View.VISIBLE);

        showLiveResultStatistics();
    }

    private void refreshPreGoLiveUI() {
        mGoLiveLayout.setVisibility(View.VISIBLE);
        mGoLiveLabel.setVisibility(View.VISIBLE);
        mLiveSettings.setVisibility(View.VISIBLE);
    }

    private void startToGetViews() {
        if (mLiveViewsTimer != null) {
            mLiveViewsTimer.schedule(
                    new TimerTask() {
                        public void run() {
                            LiveMainFragment.this.startGetViews();
                        }
                    }, LIVE_INFO_REFRESH_INTERVAL);
        }
    }

    private void startGetViews() {
        FbUtil.getLiveViews(mLiveViewsListener, mLiveInfoCacheBean.getLiveStreamId());
    }

    private void stopGetViews() {
        if (mLiveViewsTimer != null) {
            mLiveViewsTimer.cancel();;
            mLiveViewsTimer = null;
        }
    }

    private void updateLiveViewers() {
        LiveViews liveViews = mLiveInfoCacheBean.getLiveViews();
        if (liveViews != null) {
            mLiveViews.setText(Util.getFormattedNumber(liveViews.getLiveViews()));
        }
    }

    private void updateTotalViewers() {
        TextView totalViewers = (TextView) mResultLayout.findViewById(R.id.total_viewers);
        LiveViews liveViews = mLiveInfoCacheBean.getLiveViews();
        if (liveViews != null) {
            totalViewers.setText(Util.getFormattedNumber(liveViews.getTotalViews()));
        } else {
            totalViewers.setText(Util.getFormattedNumber(0));
        }
    }

    private void startToGetComment() {
        if (mLiveCommentsTimer != null) {
            mLiveCommentsTimer.schedule(
                    new TimerTask() {
                        public void run() {
                            LiveMainFragment.this.startGetComment();
                        }
                    }, LIVE_INFO_REFRESH_INTERVAL);
        }
    }

    private void startGetComment() {
        FbUtil.getLiveComments(mLiveCommentListener, mLiveInfoCacheBean.getLiveStreamId(),
                FbUtil.PAGE_SIZE, mLiveInfoCacheBean.getLiveCommentCursor());
    }

    private void stopGetComment() {
        if (mLiveCommentsTimer != null) {
            mLiveCommentsTimer.cancel();
            mLiveCommentsTimer = null;
        }
    }

    private void updateLiveComments() {
        mLiveComments.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalComments()));

        // Update comment list
        mCommentAdapter.setCommentData(mLiveInfoCacheBean.getLiveComments());
        mCommentAdapter.notifyDataSetChanged();
    }

    private void updateTotalComments() {
        //Just show the number of comments
        TextView totalComments = (TextView) mResultLayout.findViewById(R.id.total_comments);
        totalComments.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalComments()));
    }

    private void startToGetReaction() {
        if (mLiveReactionsTimer != null) {
            mLiveReactionsTimer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            LiveMainFragment.this.startGetReaction();
                        }
                    }, LIVE_INFO_REFRESH_INTERVAL);
        }
    }

    private void startGetReaction() {
        FbUtil.getLiveReactions(mLiveReactionsListener, mLiveInfoCacheBean.getLiveStreamId(),
                FbUtil.PAGE_SIZE, mLiveInfoCacheBean.getLiveReactionCursor());
    }

    private void stopGetReaction() {
        if (mLiveReactionsTimer != null) {
            mLiveReactionsTimer.cancel();
            mLiveReactionsTimer = null;
        }
    }

    private void updateLiveReactions() {
        if (mReactionView != null) {
            mReactionView.start();

            mReactionView.addReactions(mLiveInfoCacheBean.getLiveReactions());
            mLiveInfoCacheBean.clearLiveReactions();
        }
    }

    private void updateTotalLikes() {
        //Just show the number of likes
        TextView totalLikes = (TextView) mResultLayout.findViewById(R.id.total_likes);
        totalLikes.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalLikes()));
    }

    private void showLiveDuration() {
        TextView duration = (TextView)mResultLayout.findViewById(R.id.live_duration);
        String time = getResources().getString(R.string.total_live_time, mLiveTimer.getTimeStr());
        duration.setText(time);
    }

    private void showLiveResultStatistics() {
        showLiveDuration();
        updateTotalViewers();
        updateTotalComments();
        updateTotalLikes();
        clearResultInfoCache();
    }

    private void hideResultInfo() {
        mResultLayout.setVisibility(View.GONE);
        refreshPreGoLiveUI();
        clearResultViewCache();
    }

    private void clearResultInfoCache() {
        mLiveInfoCacheBean.clearComments();
        mLiveInfoCacheBean.clearReactions();
    }

    private void clearResultViewCache() {
        mLiveComments.setText(null);
        mLiveViews.setText(null);
    }

    private void deleteLiveVideo() {
        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.deleteLive(new FbUtil.OnDataRetrievedListener<Boolean>() {

            @Override
            public void onSuccess(Boolean data) {
                mLoadingLayout.setVisibility(View.GONE);
                hideResultInfo();
                showLiveDeletedDialog();
            }

            @Override
            public void onError(Exception exp) {
                mLoadingLayout.setVisibility(View.GONE);
                Log.e(LOG_TAG, "Delete video failed!");
                exp.printStackTrace();
                hideResultInfo();
            }
        }, mLiveInfoCacheBean.getLiveStreamId());
    }

    private void postLiveVideo() {
        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.updateLive(new FbUtil.OnDataRetrievedListener<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                mLoadingLayout.setVisibility(View.GONE);
                hideResultInfo();
                showLivePostedDialog();
            }

            @Override
            public void onError(Exception exp) {
                mLoadingLayout.setVisibility(View.GONE);
                Log.e(LOG_TAG, "Post video failed!");
                exp.printStackTrace();
                hideResultInfo();
            }
        }, mLiveInfoCacheBean.getLiveStreamId(), mPrivacyCacheBean.toJsonString());
    }

    private void logoutFromFacebook(boolean switchAccount) {
        if (getActivity() == null) {
            return;
        }

        LoginManager.getInstance().logOut();
        if (switchAccount) {
            getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
        }
        getActivity().finish();
    }

    // Implementation of View.OnClickListener
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_user_info:
                showLogoutDialog();
                break;
            case R.id.layout_privacy_setting:
                startActivityForResult(new Intent(getActivity(), TimelineActivity.class),
                        REQUEST_LIVE_PRIVACY);
                break;
            case R.id.btn_switch_camera:
                mPublisher.switchCameraFace(
                        (mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
                break;
            case R.id.btn_capture:
                break;
            case R.id.btn_select_camera:
                break;
            case R.id.btn_camera_mode:
                break;
            case R.id.btn_record_mute:
                mPublisher.setSendVideoOnly(!v.isSelected());
                v.setSelected(!v.isSelected());
                break;
            case R.id.btn_go_live:
                if (v.isSelected()) {
                    v.setSelected(false);
                    stopLive();
                } else {
                    startGoLive();
                }
                break;
            case R.id.live_comments_view:
                updateLiveComments();
                break;
            case R.id.privacy_view:
                startActivityForResult(new Intent(getActivity(), TimelineActivity.class),
                        REQUEST_LIVE_PRIVACY);
                break;
            case R.id.btn_post_live:
                postLiveVideo();
                break;
            case R.id.btn_delete_live:
                deleteLiveVideo();
                break;
            case R.id.btn_positive:
            case R.id.btn_negative:
                handleLivePostedDialogOnClick(v.getId());
                break;
            default:
                break;
        }
    }

    // Implementation of SrsRtmpListener
    @Override
    public void onRtmpConnecting(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public void onRtmpConnected(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Log.d(LOG_TAG, "RTMP stopped");
    }

    @Override
    public void onRtmpDisconnected() {
        Log.d(LOG_TAG, "RTMP disconnected");
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.d(LOG_TAG, String.format("RTMP output fps changed to %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(LOG_TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(LOG_TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(LOG_TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(LOG_TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler
    @Override
    public void onRecordPause() {
        Log.d(LOG_TAG, "onRecordPause");
    }

    @Override
    public void onRecordResume() {
        Log.d(LOG_TAG, "onRecordResume");
    }

    @Override
    public void onRecordStarted(String msg) {
        Log.d(LOG_TAG, "onRecordStarted: " + msg);
    }

    @Override
    public void onRecordFinished(String msg) {
        Log.d(LOG_TAG, "onRecordFinished: " + msg);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler
    @Override
    public void onNetworkWeak() {
        Log.d(LOG_TAG, "Network weak");
    }

    @Override
    public void onNetworkResume() {
        Log.d(LOG_TAG, "Network resume");
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }
}
