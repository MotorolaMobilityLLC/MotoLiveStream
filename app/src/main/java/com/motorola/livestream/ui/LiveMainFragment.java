package com.motorola.livestream.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.motorola.livestream.util.CircleTransform;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;

public class LiveMainFragment extends Fragment
        implements SrsEncodeHandler.SrsEncodeListener, RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, View.OnClickListener {

    private static final String LOG_TAG = "LiveMainFragment";

    private static final int REQUEST_LIVE_PRIVACY = 0x198;
    private static final int REQUEST_LIVE_RESULT_PRIVACY = 0x199;

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

    private View mLoadingLayout;

    private View mTopLayout;
    private LiveCountingTimer mLiveTimer;

    private View mLiveSettings;
    private View mUserInfoLayout;
    private ImageView mUserAvatar;
    private TextView mUserName;
    private ImageView mPrivacyIcon;
    private TextView mPrivacyTitle;
    private EditText mLiveInfoInput;

    private View mGoLiveLayout;
    private ImageButton mBtnGoLive;
    private TextView mGoLiveLabel;
    private ImageButton mBtnExit;

    private View mLiveInteract;
    private TextView mLiveComments;
    private TextView mLiveViews;

    private View mResultLayout;
    private TextView mLiveResultPrivacy;
    private ImageView mResultPrivacyIcon;

    private View mCommentLayout;
    private ReactionView mReactionView;
    private CommentListAdapter mCommentAdapter;

    private TimelinePrivacyCacheBean mPrivacyCacheBean = null;
    private LiveInfoCacheBean mLiveInfoCacheBean = null;

    private boolean mIsOnLive = false;

    private Timer mLiveCommentsTimer;
    private final OnPagedListRetrievedListener<Comment> mLiveCommentListener =
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
                    Log.w(LOG_TAG, "Get live comments failed: "
                            + exp.getMessage() + ", wait to re-get");
                    LiveMainFragment.this.startToGetComment();
                }
            };

    private Timer mLiveViewsTimer;
    private final OnDataRetrievedListener<LiveViews> mLiveViewsListener =
            new OnDataRetrievedListener<LiveViews>() {
                @Override
                public void onSuccess(LiveViews data) {
                    mLiveInfoCacheBean.setLiveViews(data);
                    mHandler.sendEmptyMessage(MSG_UPDATE_LIVE_VIEWS);
                    LiveMainFragment.this.startToGetViews();
                }

                @Override
                public void onError(Exception exp) {
                    Log.w(LOG_TAG, "Get live views failed: "
                            + exp.getMessage() + ", wait to re-get");
                    LiveMainFragment.this.startToGetViews();
                }
            };

    private Timer mLiveReactionsTimer;
    private final OnPagedListRetrievedListener<Reaction> mLiveReactionsListener =
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
                    Log.w(LOG_TAG, "Get live reactions failed: "
                            + exp.getMessage() + ", wait to re-get");
                    startToGetReaction();
                }
            };

    private final Handler mHandler = new Handler() {
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

    private final DialogInterface.OnClickListener mResumeDialogListener =
            (DialogInterface dialog, int which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mPublisher.setSendVideoOnly(false);
                        // Cause the camera is closed when onPause(), and the encoding also stopped
                        // So we have to start the camera preview, and resume encoding work
                        mPublisher.startCameraAndResumeEnc();

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
                    if (currentProfile == null) {
                        Toast.makeText(getActivity(),
                                R.string.label_profile_not_available, Toast.LENGTH_SHORT).show();
                        mUserInfoLayout.setVisibility(View.GONE);
                    } else {
                        updateUserInfo(currentProfile);
                    }
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
        // Get the real screen size and set as preview resolution

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        wm.getDefaultDisplay().getRealSize(screenSize);
        mPublisher.setPreviewResolution(screenSize.y, screenSize.x);
        mPublisher.setOutputResolution(720, 1280);
        if (mPublisher.getCamraId() != 2) {
            mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % 2);
        } else {
            mPublisher.setPreviewResolution(2160, 1080);
            mPublisher.setOutputResolution(640, 1280);
        }
        mPublisher.setVideoHDMode();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsOnLive) {
            //Popup a dialog to indicate user whether to resume or stop the live
            showResumeDialog();
        } else {
            mPublisher.startCamera();

            updateUserInfo(Profile.getCurrentProfile());
            updateLivePrivacySettings();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLiveTimer != null) {
            mLiveTimer.pauseCounting();
        }
        if (mReactionView != null) {
            // Stop reaction animation and clear reactions
            mReactionView.stop();
            mReactionView.clear();
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
        } else if (requestCode == REQUEST_LIVE_RESULT_PRIVACY) {
            if (resultCode ==RESULT_OK) {
                updateLiveResultPrivacySettings();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initWidgets(View view) {
        mLoadingLayout = view.findViewById(R.id.create_live_loading);

        // Live top layout
        mTopLayout = view.findViewById(R.id.layout_top);
        mLiveTimer = (LiveCountingTimer) mTopLayout.findViewById(R.id.live_timer_view);
        mTopLayout.findViewById(R.id.btn_record_mute).setOnClickListener(this);

        mLiveSettings = view.findViewById(R.id.layout_live_settings);
        // User information
        mUserInfoLayout = mLiveSettings.findViewById(R.id.layout_user_info);
        mUserInfoLayout.setOnClickListener(this);
        mUserAvatar = (ImageView) mUserInfoLayout.findViewById(R.id.user_avatar);
        mUserName = (TextView) mUserInfoLayout.findViewById(R.id.user_name);

        // Privacy settings
        View privacyLayout = mLiveSettings.findViewById(R.id.layout_privacy_setting);
        mPrivacyIcon = (ImageView) privacyLayout.findViewById(R.id.privacy_icon);
        mPrivacyTitle = (TextView) privacyLayout.findViewById(R.id.privacy_title);
        privacyLayout.setOnClickListener(this);

        // Live description input
        mLiveInfoInput = (EditText) mLiveSettings.findViewById(R.id.live_description_input);

        // Go live controller layout
        mGoLiveLayout = view.findViewById(R.id.layout_go_live);
        mBtnGoLive = (ImageButton) mGoLiveLayout.findViewById(R.id.btn_go_live);
        mBtnGoLive.setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_switch_camera).setOnClickListener(this);
        mGoLiveLayout.findViewById(R.id.btn_select_camera).setOnClickListener(this);
        mBtnExit = (ImageButton) mGoLiveLayout.findViewById(R.id.btn_exit);
        mBtnExit.setOnClickListener(this);
        mGoLiveLabel = (TextView) mGoLiveLayout.findViewById(R.id.label_golive);
        mGoLiveLabel.setVisibility(View.VISIBLE);

        // Live interact layout
        mLiveInteract = view.findViewById(R.id.layout_live_interact);
        mLiveComments = (TextView) mLiveInteract.findViewById(R.id.live_comments_view);
        mLiveViews = (TextView) mLiveInteract.findViewById(R.id.live_views_view);

        // Live result layout
        mResultLayout = view.findViewById(R.id.layout_live_result);
        mResultLayout.findViewById(R.id.btn_post_live).setOnClickListener(this);
        mResultLayout.findViewById(R.id.btn_delete_live).setOnClickListener(this);
        View resultPrivacyView = view.findViewById(R.id.result_privacy_setting);
        resultPrivacyView.setOnClickListener(this);
        mLiveResultPrivacy = (TextView) resultPrivacyView.findViewById(R.id.result_privacy_view);
        mResultPrivacyIcon = (ImageView) resultPrivacyView.findViewById(R.id.result_privacy_icon);

        // Live comment and reaction layout
        mCommentLayout = view.findViewById(R.id.layout_live_comments);
        mReactionView = (ReactionView) mCommentLayout.findViewById(R.id.reaction_view);
        RecyclerView commentListView = (RecyclerView) mCommentLayout.findViewById(R.id.comment_list);
        commentListView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mCommentAdapter = new CommentListAdapter(commentListView);
        commentListView.setAdapter(mCommentAdapter);
    }

    private void updateUserInfo(Profile profile) {
        if (profile == null) {
            // Wait ProfileTracker to update the current profile
            Log.w(LOG_TAG, "Current profile is null, wait ProfileTracker to update");
            mUserInfoLayout.setVisibility(View.GONE);
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
            mUserInfoLayout.setVisibility(View.VISIBLE);
            mUserName.setText(currentUser.getName());
            updateUserPhoto(currentUser);
        }
    }

    private void updateUserPhoto(User currentUser) {
        if (getActivity() == null) {
            return;
        }

        if (TextUtils.isEmpty(currentUser.getUserPhotoUrl())) {
            String cachedUrl = SettingsPref.getUserPhotoUrl(getActivity());
            if (!TextUtils.isEmpty(cachedUrl)) {
                Glide.with(getActivity())
                        .load(cachedUrl)
                        .placeholder(R.drawable.ic_user_photo_default)
                        .transform(new CircleTransform(getActivity()))
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
                            Log.w(LOG_TAG, "Get user photo failed: " + exp.getMessage());
                        }
                    }, currentUser);
        } else {
            String newUrl = currentUser.getUserPhotoUrl();
            String cachedUrl = SettingsPref.getUserPhotoUrl(getActivity());
            if (cachedUrl == null
                    || !cachedUrl.equals(newUrl)) {
                SettingsPref.saveUserPhotoUrl(getActivity(), newUrl);
            }

            Glide.with(getActivity())
                    .load(newUrl)
                    .placeholder(R.drawable.ic_user_photo_default)
                    .transform(new CircleTransform(getActivity()))
                    .into(mUserAvatar);
        }
    }

    private void updateLivePrivacySettings() {
        if (mPrivacyCacheBean == null) {
            mPrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_TIMELINE_PRIVACY);
        }

        mPrivacyIcon.setImageResource(mPrivacyCacheBean.getPrivacyIcon(false));
        switch (mPrivacyCacheBean.getPrivacy()) {
            case CUSTOM:
                mPrivacyTitle.setText(mPrivacyCacheBean.getCustomFriendListDisplay());
                break;
            default:
                mPrivacyTitle.setText(mPrivacyCacheBean.getPrivacyTitle());
        }
    }

    private void updateLiveResultPrivacySettings() {
        if (mPrivacyCacheBean == null) {
            mPrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_TIMELINE_PRIVACY);
        }

        mResultPrivacyIcon.setImageResource(mPrivacyCacheBean.getPrivacyIcon(false));
        switch (mPrivacyCacheBean.getPrivacy()) {
            case CUSTOM:
                mLiveResultPrivacy.setText(
                        mPrivacyCacheBean.getCustomFriendListDisplay());
                break;
            default:
                mLiveResultPrivacy.setText(mPrivacyCacheBean.getPrivacyTitle());
        }
    }

    private void handleException(Exception e) {
        try {
            e.printStackTrace();

            mPublisher.stopPublish();
            mPublisher.stopRecord();
        } catch (Exception e1) {
        }
    }

    private void showLogoutDialog() {
        User currentUser = mLiveInfoCacheBean.getUser();
        if (currentUser == null) {
            return;
        }

        final FragmentActivity activity = getActivity();

        String userName = currentUser.getName();
        String message = activity.getString(R.string.live_dlg_logout_message, userName);
        int startIndex = message.indexOf(userName);
        SpannableStringBuilder ssb = new SpannableStringBuilder(message);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + userName.length(),
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

        new AlertDialog.Builder(activity)
                .setMessage(ssb)
                .setPositiveButton(R.string.live_dlg_btn_logout,
                        (DialogInterface dialog, int which) -> logoutFromFacebook(false))
                .show();
    }

    private void showResumeDialog() {
        if (getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.live_popup_dlg_title)
                .setMessage(R.string.live_popup_dlg_content)
                .setNegativeButton(R.string.live_popup_dlg_btn_finish, mResumeDialogListener)
                .setPositiveButton(R.string.live_popup_dlg_btn_resume, mResumeDialogListener)
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
                            mPublisher.startCamera();
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
        } else {
            // If not jump to facebook, resume the camera preview
            mPublisher.startCamera();
        }
    }

    public void startGoLive() {
        User currentUser = mLiveInfoCacheBean.getUser();
        if (currentUser == null) {
            Toast.makeText(getActivity(),
                    R.string.label_profile_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

//        if (!((LiveDynamicActivity) getActivity()).checkIfNeedToRequestPermission()) {
//            // The checkIfNeedToRequestPermission function will handle the login procedure
//            // So just return here
//            return;
//        }

        // Hide the soft input panel
        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLiveInfoInput.getWindowToken(), 0);

        mLoadingLayout.setVisibility(View.VISIBLE);

        Boolean pano = false;
        if (mPublisher.getCamraId() == 2) {
            pano = true;
        }

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
                currentUser.getId(), mLiveInfoInput.getText().toString(),
                mPrivacyCacheBean.toJsonString(), pano);
    }

    private void onLiveStreamReady(LiveInfo liveInfo) {
        if (mLiveInfoCacheBean == null) {
            mLiveInfoCacheBean = (LiveInfoCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_LIVE_INFO);
        }
        mLiveInfoCacheBean.setLiveInfo(liveInfo);

        if (mCommentAdapter != null) {
            mCommentAdapter.clearData();
            mCommentAdapter.notifyDataSetChanged();
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

        // Reset comment/live viewer view
        mLiveComments.setText(Util.getFormattedNumber(0));
        mLiveViews.setText(Util.getFormattedNumber(0));
        mLiveInteract.setVisibility(View.VISIBLE);

        mBtnGoLive.setSelected(true);
        mGoLiveLabel.setVisibility(View.GONE);

        mBtnExit.setVisibility(View.GONE);

        // Reset send audio/video only mode
        mPublisher.setSendAudioOnly(false);
        mPublisher.setSendVideoOnly(false);

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
        if (mLiveCommentsTimer != null) {
            mLiveCommentsTimer.cancel();
            mLiveCommentsTimer = null;
        }

        if (mLiveViewsTimer != null) {
            mLiveViewsTimer.cancel();
            mLiveViewsTimer = null;
        }

        if (mLiveReactionsTimer != null) {
            mLiveReactionsTimer.cancel();
            mLiveReactionsTimer = null;
        }
    }

    private void stopLive() {
        mIsOnLive = false;
        mLoadingLayout.setVisibility(View.VISIBLE);
        stopUpdateInteractInfo();
        mLiveTimer.stopCounting();
        // Stop reaction animation and clear reactions
        mReactionView.stop();
        mReactionView.clear();

        FbUtil.stopLiveVideo(
                new FbUtil.OnDataRetrievedListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        mHandler.sendEmptyMessage(MSG_LIVE_STOPPED);
                    }

                    @Override
                    public void onError(Exception exp) {
                        Log.w(LOG_TAG, "Stop live video failed: " + exp.getMessage());
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

        // Hide the interact layout
        mLiveInteract.setVisibility(View.GONE);

        // Hide the comment layout
        mCommentLayout.setVisibility(View.GONE);

        // Show live result dialog
        mResultLayout.setVisibility(View.VISIBLE);

        showLiveResultInfo();
    }

    private void refreshPreGoLiveUI() {
        mGoLiveLayout.setVisibility(View.VISIBLE);
        mGoLiveLabel.setVisibility(View.VISIBLE);
        mLiveSettings.setVisibility(View.VISIBLE);
        mBtnExit.setVisibility(View.VISIBLE);
    }

    private void startToGetViews() {
        if (mLiveViewsTimer == null) {
            mLiveViewsTimer = new Timer();
        }

        mLiveViewsTimer.schedule(
                new TimerTask() {
                    public void run() {
                        LiveMainFragment.this.startGetViews();
                    }
                }, LIVE_INFO_REFRESH_INTERVAL);
    }

    private void startGetViews() {
        FbUtil.getLiveViews(mLiveViewsListener, mLiveInfoCacheBean.getLiveStreamId());
    }

    private void updateLiveViewers() {
        LiveViews liveViews = mLiveInfoCacheBean.getLiveViews();
        if (liveViews != null) {
            mLiveViews.setText(Util.getFormattedNumber(liveViews.getLiveViews()));
        }
    }

    private void startToGetComment() {
        if (mLiveCommentsTimer == null) {
            mLiveCommentsTimer = new Timer();
        }

        mLiveCommentsTimer.schedule(
                new TimerTask() {
                    public void run() {
                        LiveMainFragment.this.startGetComment();
                    }
                }, LIVE_INFO_REFRESH_INTERVAL);
    }

    private void startGetComment() {
        FbUtil.getLiveComments(mLiveCommentListener, mLiveInfoCacheBean.getLiveStreamId(),
                FbUtil.PAGE_SIZE, mLiveInfoCacheBean.getLiveCommentCursor());
    }

    private void updateLiveComments() {
        mLiveComments.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalComments()));

        // Update comment list
        mCommentAdapter.setCommentData(mLiveInfoCacheBean.getLiveComments());
        mCommentAdapter.notifyDataSetChanged();
    }

    private void startToGetReaction() {
        if (mLiveReactionsTimer == null) {
            mLiveReactionsTimer = new Timer();
        }

        mLiveReactionsTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        LiveMainFragment.this.startGetReaction();
                    }
                }, LIVE_INFO_REFRESH_INTERVAL);
    }

    private void startGetReaction() {
        FbUtil.getLiveReactions(mLiveReactionsListener, mLiveInfoCacheBean.getLiveStreamId(),
                FbUtil.PAGE_SIZE, mLiveInfoCacheBean.getLiveReactionCursor());
    }

    private void updateLiveReactions() {
        if (mReactionView != null) {
            mReactionView.start();

            mReactionView.addReactions(mLiveInfoCacheBean.getLiveReactions());
            mLiveInfoCacheBean.clearLiveReactions();
        }
    }

    private void showLiveResultInfo() {
        // Show the live duration
        TextView duration = (TextView)mResultLayout.findViewById(R.id.live_duration);
        String time = getResources().getString(R.string.total_live_time, mLiveTimer.getTimeStr());
        duration.setText(time);

        // Show the total number of viewers
        TextView totalViewers = (TextView) mResultLayout.findViewById(R.id.total_viewers);
        LiveViews liveViews = mLiveInfoCacheBean.getLiveViews();
        if (liveViews != null) {
            totalViewers.setText(Util.getFormattedNumber(liveViews.getTotalViews()));
        } else {
            totalViewers.setText(Util.getFormattedNumber(0));
        }

        // Show the total number of comments
        TextView totalComments = (TextView) mResultLayout.findViewById(R.id.total_comments);
        totalComments.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalComments()));

        // Show the total number of likes
        TextView totalLikes = (TextView) mResultLayout.findViewById(R.id.total_likes);
        totalLikes.setText(Util.getFormattedNumber(mLiveInfoCacheBean.getTotalLikes()));

        // Show the current privacy settings
        updateLiveResultPrivacySettings();

        clearResultInfoCache();
    }

    private void hideResultInfo() {
        mLoadingLayout.setVisibility(View.GONE);

        mResultLayout.setVisibility(View.GONE);
        refreshPreGoLiveUI();
    }

    private void clearResultInfoCache() {
        mLiveInfoCacheBean.clearComments();
        mLiveInfoCacheBean.clearReactions();
    }

    private void deleteLiveVideo() {
        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.deleteLive(new FbUtil.OnDataRetrievedListener<Boolean>() {

            @Override
            public void onSuccess(Boolean data) {
                hideResultInfo();
                showLiveDeletedDialog();
            }

            @Override
            public void onError(Exception exp) {
                Log.w(LOG_TAG, "Delete video failed: " + exp.getMessage());
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
                hideResultInfo();
                showLivePostedDialog();
            }

            @Override
            public void onError(Exception exp) {
                Log.e(LOG_TAG, "Post video failed: " + exp.getMessage());
                exp.printStackTrace();
                hideResultInfo();
            }
        }, mLiveInfoCacheBean.getLiveStreamId(), mPrivacyCacheBean.toJsonString());
    }

    private void logoutFromFacebook(boolean switchAccount) {
        if (getActivity() == null) {
            return;
        }

        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.deAuthorize(new OnDataRetrievedListener<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                LoginManager.getInstance().logOut();
                if (switchAccount) {
                    getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
                }
                getActivity().finish();
            }

            @Override
            public void onError(Exception exp) {
                Log.w(LOG_TAG, "Delete user permission failed: " + exp.getMessage());
                LoginManager.getInstance().logOut();
                getActivity().finish();
            }
        });
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
                if (mPublisher.getCamraId() < 2) {
//                    mPublisher.switchCameraFace(
//                            (mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
                    mPublisher.switchCameraFace(
                            (mPublisher.getCamraId() + 1) % 2);
                }
            case R.id.btn_select_camera:
                break;
            case R.id.btn_exit:
                getActivity().finish();
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
            case R.id.result_privacy_setting:
                startActivityForResult(new Intent(getActivity(), TimelineActivity.class),
                        REQUEST_LIVE_RESULT_PRIVACY);
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
        Log.d(LOG_TAG, String.format(Locale.ENGLISH, "RTMP output fps changed to %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(LOG_TAG, String.format(Locale.ENGLISH, "Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(LOG_TAG, String.format(Locale.ENGLISH, "Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(LOG_TAG, String.format(Locale.ENGLISH, "Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(LOG_TAG, String.format(Locale.ENGLISH, "Audio bitrate: %d bps", rate));
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
