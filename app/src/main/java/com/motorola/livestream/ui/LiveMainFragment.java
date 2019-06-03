package com.motorola.livestream.ui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;

import com.motorola.cameramod360.SphereCameraView;
import com.motorola.cameramod360.SphereMediaView;
import com.motorola.cameramod360.gl.SphereViewRenderer;
import com.motorola.cameramod360.gl.SphereViewRenderer.ViewType;

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
import com.motorola.livestream.util.ModHelper;
import com.motorola.livestream.util.SettingsPref;
import com.motorola.livestream.util.Util;
import com.motorola.livestream.viewcache.ViewCacheManager;
import com.motorola.livestream.viewcache.fb.LiveInfoCacheBean;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublishListener;
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
        implements SrsEncodeHandler.SrsEncodeListener, SrsPublishListener,
        SrsRecordHandler.SrsRecordListener, View.OnClickListener {

    private static final String LOG_TAG = "LiveMainFragment";

    private static final int REQUEST_LIVE_PRIVACY = 0x198;
    private static final int REQUEST_LIVE_RESULT_PRIVACY = 0x199;

    private static final int LIVE_INFO_REFRESH_INTERVAL = 3000; //3sec

    private static final int MSG_START_LIVE = 0x101;
    private static final int MSG_LIVE_CONNECTED = 0x102;
    private static final int MSG_UPDATE_LIVE_COMMENTS = 0x103;
    private static final int MSG_UPDATE_LIVE_VIEWS = 0x104;
    private static final int MSG_UPDATE_LIVE_REACTIONS = 0x105;
    private static final int MSG_LIVE_STOPPED = 0x106;
    private static final int MSG_EXIT_APP = 0x107;

    private static final int MSG_RETRY_OPEN_CAMERA = 0x121;
    private static final int MSG_RETRY_START_CAMERA = 0x122;
    private static final int MSG_RETRY_RESUME_CAMERA = 0x123;

    private static final int MSG_CREATE_LIVE_TIME_OUT = 0x201;
    private static final int MSG_CONNECT_LIVE_TIME_OUT = 0x202;

    private static final int MSG_PUSH_LIVE_TIME_OUT = 0x301;

    private static final int MSG_DIM_SCREEN_TIME_OUT = 0x401;

    private static final long CREATE_LIVE_TIME_OUT = 10000L;
    private static final long CONNECT_LIVE_TIME_OUT = 5000L;
    private static final long PUSH_LIVE_TIME_OUT = 5000L;
    private static final long DIM_SCREEN_TIME_OUT = 60000L;

    private static final int MOTO_360_MOD_CAMERA = 2;

    private static final int OPEN_CAMERA_RETRY_MAX_COUNT = 5;
    private static final long OPEN_CAMERA_RETRY_TIME = 200L;

    private enum LiveStatus {
        PRE_GO_LIVE,
        CREATING_LIVE,
        CREATE_LIVE_FAILED,
        CONNECTING,
        CONNECT_FAILED,
        LIVING,
        END
    }

    public static Fragment newInstance() {
        return new LiveMainFragment();
    }

    private int mDefaultCamId = -1;
    private SrsPublisher mPublisher = null;
    private SphereCameraView mSphereCameraView;
    private ImageView mBtnSphereSwitch;

    private View mLoadingLayout;

    private View mTopLayout;
    private View mLicenseLayout;
    private LiveCountingTimer mLiveTimer;

    private View mLiveSettings;
    private View mUserInfoLayout;
    private ImageView mUserAvatar;
    private TextView mUserName;
    private ImageView mPrivacyIcon;
    private TextView mPrivacyTitle;
    private TextInputEditText mLiveInfoInput;
    private View mLive4KSettings;
    private Switch m4KLiveSwitch;

    private View mGoLiveLayout;
    private View mBtnGoLive;
    private View mGoLiveLabel;
    private View mBtnExit;
    private View mBtnSwitchCamera;
    private View mBtnSelectCamera;

    private View mLiveInteract;
    private TextView mLiveComments;
    private TextView mLiveViews;

    private View mResultLayout;
    private TextView mLiveResultPrivacy;
    private ImageView mResultPrivacyIcon;

    private View mCommentLayout;
    private RecyclerView mCommentsView;
    private ReactionView mReactionView;
    private CommentListAdapter mCommentAdapter;

    private View mDynamicCamBtnLayout;

    private TimelinePrivacyCacheBean mPrivacyCacheBean = null;
    private LiveInfoCacheBean mLiveInfoCacheBean = null;

    private LiveStatus mLiveStatus = LiveStatus.PRE_GO_LIVE;
//    private boolean mIsOnLive = false;

    private AlertDialog mPostDialog = null;
    private AlertDialog mLogoutDialog = null;
    private AlertDialog mResumeDialog = null;

    private Timer mLiveCommentsTimer;

    private PopupWindow mPopWindow;
    private View mBtnOpenSource;

    private int mOpenCameraRetryCount = 0;
    private int mPrevAudioMode;

    private boolean mIsScreenDimming = false;

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
                case MSG_LIVE_CONNECTED:
                    showLiveStatusInfo();
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
                case MSG_EXIT_APP:
                    System.exit(0);
                    break;
                case MSG_RETRY_OPEN_CAMERA:
                    swapCamera(msg.arg1);
                    break;
                case MSG_RETRY_START_CAMERA:
                    startCameraPreview();
                    break;
                case MSG_RETRY_RESUME_CAMERA:
                    resumeLive();
                    break;
                case MSG_CREATE_LIVE_TIME_OUT:
                    muteRinger(false);
                    showLiveTimeoutDialog();
                    mLiveStatus = LiveStatus.CREATE_LIVE_FAILED;
                    break;
                case MSG_CONNECT_LIVE_TIME_OUT:
                    muteRinger(false);
                    showLiveTimeoutDialog();
                    mLiveStatus = LiveStatus.CONNECT_FAILED;
                    break;
                case MSG_PUSH_LIVE_TIME_OUT:
                    if (isDuringLive()) {
                        muteRinger(false);
                        handleNwException(null);
                        mLiveStatus = LiveStatus.CONNECT_FAILED;
                    }
                    break;
                case MSG_DIM_SCREEN_TIME_OUT:
                    if (isDuringLive()) {
                        tryDimScreen(true);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final DialogInterface.OnClickListener mResumeDialogListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            resumeLive();
                            mResumeDialog = null;
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            stopLive();
                            mBtnGoLive.setSelected(false);
                            mResumeDialog = null;
                            break;
                    }
                }
            };

    private BroadcastReceiver mMoto360Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ModHelper.ACTION_MOTO_360_ATTACHED.equals(action)
                    || ModHelper.ACTION_MOTO_360_DETACHED.equals(action)) {
                if (isDuringLive()) {
                    mPublisher.stopPublish();
                    mPublisher.stopCamera();
                }
                if (getActivity() != null) {
                    getActivity().finish();
                }
                mHandler.sendEmptyMessageDelayed(MSG_EXIT_APP, 200L);
            }
        }
    };

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

        IntentFilter filter = new IntentFilter();
        filter.addAction(ModHelper.ACTION_MOTO_360_ATTACHED);
        filter.addAction(ModHelper.ACTION_MOTO_360_DETACHED);
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mMoto360Receiver, filter);
        if (Profile.getCurrentProfile() == null) {
            new ProfileTracker() {
                @Override
                protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                    stopTracking();
                    if (currentProfile == null) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.label_profile_not_available, Toast.LENGTH_SHORT).show();
                            mUserInfoLayout.setVisibility(View.GONE);
                        }
                    } else {
                        updateUserInfo(currentProfile);
                    }
                }
            }.startTracking();
        }
        setListenerToRootView();
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

        mLiveStatus = LiveStatus.PRE_GO_LIVE;

        initDefaultCamId();

        initWidgets(view);

        mSphereCameraView = (SphereCameraView) view.findViewById(R.id.sphere_camera_view);
        mSphereCameraView.initSphereView(mDefaultCamId);
        mSphereCameraView.setOnGestureListener(new SphereMediaView.OnGestureListener() {
            @Override
            public void onDown(MotionEvent e) {
                handleDimScreen();
            }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                return false;
            }
        });

        mPublisher = new SrsPublisher(mSphereCameraView);
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpListener(this);
        mPublisher.setRecordHandler(new SrsRecordHandler(this));

        swapCamera(mDefaultCamId, true);
    }

    @Override
    public void onResume() {
        super.onResume();

        getInitAudioMode();
        if (isDuringLive()) {
            tryDimScreen(false);
            //Popup a dialog to indicate user whether to resume or stop the live
            showResumeDialog();
        } else {
            startCameraPreview();

            updateUserInfo(Profile.getCurrentProfile());
            updateLivePrivacySettings();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        tryDimScreen(false);

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
        muteRinger(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPublisher != null) {
            mPublisher.stopCamera();
        }

        if (mPopWindow != null) {
            if (mPopWindow.isShowing()) {
                mPopWindow.dismiss();
            }
        }

        mPrivacyCacheBean.clean();
        mLiveInfoCacheBean.clean();

        mLiveTimer.stopCounting();
        mLiveStatus = LiveStatus.END;

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMoto360Receiver);
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

        mLicenseLayout = view.findViewById(R.id.layout_license_info);
        mBtnOpenSource = mLicenseLayout.findViewById(R.id.btn_overflow);
        mBtnOpenSource.setOnClickListener(this);

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
        mLiveInfoInput = (TextInputEditText) mLiveSettings.findViewById(R.id.live_description_input);

        // Live 4K switch
        mLive4KSettings = mLiveSettings.findViewById(R.id.layout_4k_setting);
        m4KLiveSwitch = (Switch) mLive4KSettings.findViewById(R.id.settings_4k_switch);
        mLive4KSettings.setOnClickListener(this);
        m4KLiveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                swapCamera(mPublisher.getCameraId(), true);
            }
        });

        // Go live controller layout
        mGoLiveLayout = view.findViewById(R.id.layout_go_live);
        mBtnGoLive = mGoLiveLayout.findViewById(R.id.btn_go_live);
        mBtnGoLive.setOnClickListener(this);
        mBtnSwitchCamera = mGoLiveLayout.findViewById(R.id.btn_switch_camera);
        mBtnSwitchCamera.setOnClickListener(this);
        mBtnSelectCamera = mGoLiveLayout.findViewById(R.id.btn_select_camera);
        mBtnSelectCamera.setOnClickListener(this);
        mBtnSphereSwitch = (ImageView) mGoLiveLayout.findViewById(R.id.btn_sphere_switch);
        mBtnSphereSwitch.setOnClickListener(this);
        mBtnSphereSwitch.setVisibility((mDefaultCamId == MOTO_360_MOD_CAMERA)
                ? View.VISIBLE : View.GONE);

        mBtnExit = mGoLiveLayout.findViewById(R.id.btn_exit);
        mBtnExit.setOnClickListener(this);
        mGoLiveLabel = mGoLiveLayout.findViewById(R.id.label_golive);
        mGoLiveLabel.setVisibility(View.VISIBLE);

        // Live interact layout
        mLiveInteract = view.findViewById(R.id.layout_live_interact);
        mLiveComments = (TextView) mLiveInteract.findViewById(R.id.live_comments_view);
        mLiveViews = (TextView) mLiveInteract.findViewById(R.id.live_views_view);

        // Live result layout
        mResultLayout = view.findViewById(R.id.layout_live_result);
        mResultLayout.findViewById(R.id.btn_goto_fb).setOnClickListener(this);
        mResultLayout.findViewById(R.id.btn_delete_live).setOnClickListener(this);
        View resultPrivacyView = view.findViewById(R.id.result_privacy_setting);
        resultPrivacyView.setOnClickListener(this);
        mLiveResultPrivacy = (TextView) resultPrivacyView.findViewById(R.id.result_privacy_view);
        mResultPrivacyIcon = (ImageView) resultPrivacyView.findViewById(R.id.result_privacy_icon);

        // Live comment and reaction layout
        mCommentLayout = view.findViewById(R.id.layout_live_comments);
        mReactionView = (ReactionView) mCommentLayout.findViewById(R.id.reaction_view);
        mCommentsView = (RecyclerView) mCommentLayout.findViewById(R.id.comment_list);
        mCommentsView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mCommentAdapter = new CommentListAdapter(mCommentsView);
        mCommentsView.setAdapter(mCommentAdapter);
        mCommentsView.setVisibility(View.GONE);

        //Dynamic camera button to show phone camera or camera mod
        mDynamicCamBtnLayout = view.findViewById(R.id.dynamic_cam_btn);
        mDynamicCamBtnLayout.setOnClickListener(this);
        mDynamicCamBtnLayout.findViewById(R.id.phone_cam).setOnClickListener(this);
        mDynamicCamBtnLayout.findViewById(R.id.mod_cam).setOnClickListener(this);

        if (isRtl()) {
            ImageView mPrivacyChevron = (ImageView) privacyLayout.findViewById(R.id.privacy_chevron);
            ImageView mUserChevron = (ImageView) mUserInfoLayout.findViewById(R.id.user_chevron);
            ImageView mResultChevronIcon = (ImageView) resultPrivacyView.findViewById(R.id.result_chevron_icon);
            Drawable arrow = getResources().getDrawable(R.drawable.item_row_chevron_white);
            if (arrow != null) {
                arrow.setAutoMirrored(true);
            }
            mPrivacyChevron.setImageDrawable(arrow);
            mUserChevron.setImageDrawable(arrow);
            mResultChevronIcon.setImageDrawable(arrow);
        }

        // Since the "Select Camera" list, only contains "Phone camera" and "Moto360 camera"
        // We only show it when ModMoto360 is attached, ignore ModHasselblad is attached
        if (ModHelper.isModMoto360Attached()) {
            mLive4KSettings.setVisibility(
                    (mDefaultCamId == MOTO_360_MOD_CAMERA) ? View.VISIBLE: View.GONE);

            refreshCameraButton(true);

            setDynamicCamBtnState(mDefaultCamId);
        } else {
            mLive4KSettings.setVisibility(View.GONE);

            refreshCameraButton(false);
        }
    }

    private void refreshCameraButton(boolean showSelectButton) {
        if (showSelectButton) {
            mBtnSelectCamera.setVisibility(View.VISIBLE);
            mBtnSwitchCamera.setVisibility(View.GONE);
        } else {
            mBtnSelectCamera.setVisibility(View.GONE);
            mBtnSwitchCamera.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) mGoLiveLayout.getLayoutParams();
            lp.height = getResources().getDimensionPixelOffset(R.dimen.go_live_button_panel_height_small);
            mGoLiveLayout.setLayoutParams(lp);

            // For normal camera, align the Camera switch button to parent bottom
            lp = (RelativeLayout.LayoutParams) mBtnSwitchCamera.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            mBtnSwitchCamera.setLayoutParams(lp);
        }
    }

    private void showDynamicCamLayout(boolean show) {
        if (show) {
            mDynamicCamBtnLayout.setVisibility(View.VISIBLE);
            mLiveSettings.setVisibility(View.GONE);
            mGoLiveLayout.setVisibility(View.GONE);
            mLicenseLayout.setVisibility(View.GONE);
        } else {
            mDynamicCamBtnLayout.setVisibility(View.GONE);
            mLiveSettings.setVisibility(View.VISIBLE);
            mGoLiveLayout.setVisibility(View.VISIBLE);
            mLicenseLayout.setVisibility(View.VISIBLE);
            mBtnSwitchCamera.setVisibility(
                    mPublisher.getCameraId() == MOTO_360_MOD_CAMERA ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setDynamicCamBtnState(int camId) {
        if (camId == MOTO_360_MOD_CAMERA) {
            mDynamicCamBtnLayout.findViewById(R.id.phone_cam).setSelected(false);
            mDynamicCamBtnLayout.findViewById(R.id.mod_cam).setSelected(true);
        } else {
            mDynamicCamBtnLayout.findViewById(R.id.phone_cam).setSelected(true);
            mDynamicCamBtnLayout.findViewById(R.id.mod_cam).setSelected(false);
        }
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
                RequestOptions options =
                        new RequestOptions().placeholder(R.drawable.ic_user_photo_default)
                                .transform(new CircleTransform());
                Glide.with(getActivity())
                        .load(cachedUrl)
                        .apply(options)
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

            RequestOptions options =
                    new RequestOptions().placeholder(R.drawable.ic_user_photo_default)
                            .transform(new CircleTransform());

            Glide.with(getActivity())
                    .load(newUrl)
                    .apply(options)
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
        e.printStackTrace();

        mLiveTimer.stopCounting();

        try {
            mPublisher.stopPublish();

            if (isDuringLive()) {
                stopLive();
            }
        } catch (Exception e1) {}
    }

    private void handleNwException(Exception e) {
        // Do not show dialog when not living or not connected to RTMP server
        if (!isDuringLive() || mHandler.hasMessages(MSG_CONNECT_LIVE_TIME_OUT)) {
            return;
        }

        if (e != null) {
            e.printStackTrace();
        }

        mLiveTimer.stopCounting();

        try {
            mPublisher.stopPublish();
        } catch (Exception e1) { }

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.live_popup_dlg_rtmp_failed)
                .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopLive();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showLogoutDialog() {
        User currentUser = mLiveInfoCacheBean.getUser();
        if (currentUser == null) {
            return;
        }

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        String userName = currentUser.getName();
        String message = activity.getString(R.string.live_dlg_logout_message, userName);
        int startIndex = message.indexOf(userName);
        SpannableStringBuilder ssb = new SpannableStringBuilder(message);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + userName.length(),
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = LayoutInflater.from(activity).inflate(R.layout.custom_live_dialog, null);
        Button btnPositive = (Button) view.findViewById(R.id.btn_positive);
        if (btnPositive != null) {
            btnPositive.setText(R.string.live_dlg_btn_logout);
            btnPositive.setOnClickListener(this);
        }
        Button btnNegative = (Button) view.findViewById(R.id.btn_negative);
        if (btnNegative != null) {
            btnNegative.setText(R.string.live_login_identify);
            btnNegative.setOnClickListener(this);
        }

        builder.setView(view)
                .setMessage(ssb);

        mLogoutDialog = builder.show();
    }

    private void handleLogoutDialogOnClick(int id) {
        if (mLogoutDialog != null) {
            mLogoutDialog.dismiss();
            mLogoutDialog = null;
        }

        if (R.id.btn_positive == id) {
            logoutFromFacebook();
        }
    }

    private void showResumeDialog() {
        if (getActivity() == null) {
            return;
        }

        if (mResumeDialog != null) {
            return;
        }

        mResumeDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.live_popup_dlg_title)
                .setMessage(R.string.live_popup_dlg_content)
                .setNegativeButton(R.string.live_popup_dlg_btn_finish, mResumeDialogListener)
                .setPositiveButton(R.string.live_popup_dlg_btn_resume, mResumeDialogListener)
                .setCancelable(false)
                .show();
    }

    private void showCameraFailedDialog() {
        if (getActivity() == null) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.live_popup_dlg_camera_failed)
                .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showCreateLiveFailedDialog(int messageId) {
        if (getActivity() == null) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage(messageId)
                .setNegativeButton(R.string.btn_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showLiveTimeoutDialog() {
        mHandler.removeMessages(MSG_CREATE_LIVE_TIME_OUT);
        mHandler.removeMessages(MSG_CONNECT_LIVE_TIME_OUT);

        // Hide the loading view
        mLoadingLayout.setVisibility(View.GONE);

        // Stop camera encoding and publishing first
        mPublisher.stopPublish();

        if (getActivity() == null || !isResumed()) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.live_popup_dlg_network_poor)
                .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopLiveForNwPoor();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showLiveDeletedDialog() {
        if (getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.live_deleted_message)
                .setPositiveButton(R.string.live_process_identify, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mResultLayout.setVisibility(View.GONE);
                        refreshPreGoLiveUI();
                        startCameraPreview();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showLivePostedDialog() {
        if (getActivity() == null || mPostDialog != null) {
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
            startCameraPreview();
        }
    }

    public void startGoLive() {
        User currentUser = mLiveInfoCacheBean.getUser();
        if (currentUser == null) {
            Toast.makeText(getActivity(),
                    R.string.label_profile_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide the soft input panel
        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLiveInfoInput.getWindowToken(), 0);

        if (!Util.isNetworkConnected(getActivity())) {
            showLiveTimeoutDialog();
            return;
        }

        mLoadingLayout.setVisibility(View.VISIBLE);

        mLiveStatus = LiveStatus.CREATING_LIVE;
        final boolean isSpherical = (mPublisher.getCameraId() == MOTO_360_MOD_CAMERA);
        FbUtil.createUserLive(
                new FbUtil.OnDataRetrievedListener<LiveInfo>() {
                    @Override
                    public void onSuccess(LiveInfo liveInfo) {
                        Log.d(LOG_TAG, "Create live video ok.");
                        // The Facebook live server seems always set 360 live to UNPUBLISHED
                        // So we need to enforce the status to LIVE_NOW before we start to live.
                        if (isSpherical) {
                            Log.d(LOG_TAG, "Created live video is not ready, set status to LIVE_NOW.");
                            updateSphericalLiveStatus(liveInfo);
                        } else {
                            onLiveStreamReady(liveInfo);
                        }
                    }

                    @Override
                    public void onError(Exception exp) {
                        // Remove the timeout message, since it already failed
                        mHandler.removeMessages(MSG_CREATE_LIVE_TIME_OUT);

                        Log.e(LOG_TAG, "Create live video failed!");
                        exp.printStackTrace();

                        mLoadingLayout.setVisibility(View.GONE);

                        // Only show error dialog while still under CREATING_LIVE status
                        if (mLiveStatus != LiveStatus.CREATING_LIVE) {
                            return;
                        }

                        if (FbUtil.handleException(exp) == FbUtil.ERR_PERMISSION_NOT_GRANTED) {
                            showCreateLiveFailedDialog(R.string.live_popup_no_publish_permission);
                        } else {
                            showCreateLiveFailedDialog(R.string.live_popup_dlg_create_live_failed);
                        }
                        muteRinger(false);
                    }
                },
                currentUser.getId(), mLiveInfoInput.getText().toString(),
                mPrivacyCacheBean.toJsonString(), isSpherical);
        // Set 10 seconds to wait the response from Facebook server
        mHandler.sendEmptyMessageDelayed(MSG_CREATE_LIVE_TIME_OUT, CREATE_LIVE_TIME_OUT);
    }

    private void updateSphericalLiveStatus(final LiveInfo liveInfo) {
        FbUtil.updateLiveStatus(
                new OnDataRetrievedListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        onLiveStreamReady(liveInfo);
                    }

                    @Override
                    public void onError(Exception exp) {
                        // Remove the timeout message, since it already failed
                        mHandler.removeMessages(MSG_CREATE_LIVE_TIME_OUT);

                        Log.e(LOG_TAG, "Failed to update spherical live video status!");
                        exp.printStackTrace();

                        mLoadingLayout.setVisibility(View.GONE);

                        // Only show error dialog while still under CREATING_LIVE status
                        if (mLiveStatus != LiveStatus.CREATING_LIVE) {
                            return;
                        }

                        if (FbUtil.handleException(exp) == FbUtil.ERR_PERMISSION_NOT_GRANTED) {
                            showCreateLiveFailedDialog(R.string.live_popup_no_publish_permission);
                        } else {
                            showCreateLiveFailedDialog(R.string.live_popup_dlg_create_live_failed);
                        }
                        muteRinger(false);
                    }
                },
                liveInfo.getId(), LiveInfo.LIVE_NOW);
    }

    private void onLiveStreamReady(LiveInfo liveInfo) {
        // Create live from Facebook server OK
        mHandler.removeMessages(MSG_CREATE_LIVE_TIME_OUT);

        mLiveStatus = LiveStatus.CONNECTING;

        if (mLiveInfoCacheBean == null) {
            mLiveInfoCacheBean = (LiveInfoCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_LIVE_INFO);
        }
        mLiveInfoCacheBean.setLiveInfo(liveInfo);

        if (mCommentAdapter != null) {
            mCommentAdapter.clearData();
            mCommentAdapter.notifyDataSetChanged();

            mCommentsView.setVisibility(View.GONE);
        }

        mHandler.sendEmptyMessage(MSG_START_LIVE);
    }

    private void onLiveStart() {
        mLicenseLayout.setVisibility(View.GONE);
//        mTopLayout.setVisibility(View.VISIBLE);
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
        mBtnSelectCamera.setVisibility(View.GONE);

        // Reset send audio/video only mode
        mPublisher.setSendAudioOnly(false);
        mPublisher.setSendVideoOnly(false);
        mTopLayout.findViewById(R.id.btn_record_mute).setSelected(false);

        mPublisher.startCamera();
        mPublisher.startPublish(mLiveInfoCacheBean.getLiveStreamUrl());

        muteRinger(true);

        // Maybe we cannot connect to the stream url
        // so wait the RTMP connected callback, then start the real live
        // and set 5 seconds timeout to check if we can connect to the stream url
//        mLiveTimer.startCounting();
//        startUpdateInteractInfo();
        mHandler.sendEmptyMessageDelayed(MSG_CONNECT_LIVE_TIME_OUT, CONNECT_LIVE_TIME_OUT);
    }

    private void showLiveStatusInfo() {
        // Hide the loading view
        mLoadingLayout.setVisibility(View.GONE);

        // Cancel timeout waiting
        mHandler.removeMessages(MSG_CONNECT_LIVE_TIME_OUT);

        mLiveStatus = LiveStatus.LIVING;

        // Show the live timer
        mTopLayout.setVisibility(View.VISIBLE);
        mLiveTimer.startCounting();
        startUpdateInteractInfo();

        handleDimScreen();
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
        if (isDuringLive()) {
            tryDimScreen(false);
        }
        mHandler.removeMessages(MSG_DIM_SCREEN_TIME_OUT);

        mLiveStatus = LiveStatus.END;

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

        muteRinger(false);
    }

    private void stopLiveForNwPoor() {
        mLiveStatus = LiveStatus.END;

        stopUpdateInteractInfo();
        mLiveTimer.stopCounting();
        // Stop reaction animation and clear reactions
        mReactionView.stop();
        mReactionView.clear();
        clearResultInfoCache();

        // Hide go live controller layout
        mTopLayout.setVisibility(View.GONE);
        mGoLiveLayout.setVisibility(View.GONE);
        mBtnGoLive.setSelected(false);

        // Hide the interact layout
        mLiveInteract.setVisibility(View.GONE);
        // Hide the comment layout
        mCommentLayout.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(mLiveInfoCacheBean.getLiveStreamId())) {
            FbUtil.deleteLive(null, mLiveInfoCacheBean.getLiveStreamId());
        }

        refreshPreGoLiveUI();
        mPublisher.startCamera();
    }

    private void showLiveStreamResult() {
        // Hide go live controller layout
        mLicenseLayout.setVisibility(View.GONE);
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
        mLiveStatus = LiveStatus.PRE_GO_LIVE;
        muteRinger(false);

        mLiveInfoCacheBean.setLiveInfo(null);
        mGoLiveLayout.setVisibility(View.VISIBLE);
        mGoLiveLabel.setVisibility(View.VISIBLE);
        mLiveSettings.setVisibility(View.VISIBLE);
        mBtnExit.setVisibility(View.VISIBLE);
        if (ModHelper.isModMoto360Attached()) {
            mBtnSelectCamera.setEnabled(true);
            mBtnSelectCamera.setVisibility(View.VISIBLE);
        }
        mLicenseLayout.setVisibility(View.VISIBLE);
    }

    private void startToGetViews() {
        if (mLiveInfoCacheBean == null
                || TextUtils.isEmpty(mLiveInfoCacheBean.getLiveStreamId())) {
            if (mLiveViewsTimer != null) {
                mLiveViewsTimer.cancel();
                mLiveViewsTimer = null;
            }
            return;
        }

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
        if (mLiveInfoCacheBean == null
                || TextUtils.isEmpty(mLiveInfoCacheBean.getLiveStreamId())) {
            if (mLiveCommentsTimer != null) {
                mLiveCommentsTimer.cancel();
                mLiveCommentsTimer = null;
            }
            return;
        }

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
        if (mCommentAdapter.getItemCount() > 0) {
            mCommentsView.setVisibility(View.VISIBLE);
            mCommentAdapter.notifyDataSetChanged();
        } else {
            mCommentsView.setVisibility(View.GONE);
        }
    }

    private void startToGetReaction() {
        if (mLiveInfoCacheBean == null
                || TextUtils.isEmpty(mLiveInfoCacheBean.getLiveStreamId())) {
            if (mLiveReactionsTimer != null) {
                mLiveReactionsTimer.cancel();
                mLiveReactionsTimer = null;
            }
            return;
        }

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
                mPublisher.startCamera();
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
                // The live video already posted, so show the dialog no matter of the post result
                showLivePostedDialog();
            }
        }, mLiveInfoCacheBean.getLiveStreamId(), mPrivacyCacheBean.toJsonString());
    }

    private void logoutFromFacebook() {
        if (getActivity() == null) {
            return;
        }

        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.deAuthorize(new OnDataRetrievedListener<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                LoginManager.getInstance().logOut();
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

    private void initDefaultCamId() {
        // Force to check if 360Mod is attached
        ModHelper.updateModStatus(getActivity());

        if (ModHelper.isModMoto360Attached()) {
            mDefaultCamId = MOTO_360_MOD_CAMERA;
        } else {
            mDefaultCamId = 0;
        }

        int requestCamId = -1;
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            requestCamId = intent.getIntExtra(Util.EXTRA_CAMERA, requestCamId);
        }

        if (requestCamId < 0 || requestCamId > 2) {
            // Ignore because the request camera id is invalid
            Log.w(LOG_TAG, "Requesting with invalid camera id: " + requestCamId);
        } else {
            if (!ModHelper.isModMoto360Attached() && requestCamId == 2) {
                // Just ignore if 360Mod not attached and request to open it
                Log.w(LOG_TAG, "Requesting with external camera id while no external camera attached");
            } else {
                mDefaultCamId = requestCamId;
            }
        }
    }

    private void swapCamera(int camId) {
        swapCamera(camId, false);
    }

    private void swapCamera(int camId, boolean forceSwap) {
        if (mPublisher.getCameraId() == camId && !forceSwap) {
            mOpenCameraRetryCount = 0;
            return;
        }

        mPublisher.stopCamera();
        try {
            if (mBtnSphereSwitch != null) {
                mBtnSphereSwitch.setVisibility((camId == MOTO_360_MOD_CAMERA)
                        ? View.VISIBLE : View.GONE);
            }
            if (camId == MOTO_360_MOD_CAMERA) {
                mPublisher.setCameraId(camId);
                if (m4KLiveSwitch.isChecked()) {
                    mPublisher.setPreviewResolution(3840, 1920);
                    mPublisher.setOutputResolution(1920, 3840);
                    mPublisher.set360VideoHDMode(true);
                } else {
                    mPublisher.setPreviewResolution(2160, 1080);
                    mPublisher.setOutputResolution(1080, 2160);
                    mPublisher.set360VideoHDMode(false);
                }
                mPublisher.switchCameraFace(camId);

                mSphereCameraView.setViewType(ViewType.SPHERICAL, true);
                mSphereCameraView.setMediaSize();
            } else {
                mPublisher.setCameraId(camId);
                // Get the real screen size and set as preview resolution
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                Point screenSize = new Point();
                wm.getDefaultDisplay().getRealSize(screenSize);
                mPublisher.setPreviewResolution(screenSize.y, screenSize.x);
                mPublisher.setOutputResolution(720, 1280);
                mPublisher.switchCameraFace(camId);
                mPublisher.setVideoHDMode();

                mSphereCameraView.setViewType(ViewType.DEFAULT, true);
                mSphereCameraView.setMediaSize();
            }
            mOpenCameraRetryCount = 0;
        } catch (Exception e) {
            e.printStackTrace();

            if (mOpenCameraRetryCount >= OPEN_CAMERA_RETRY_MAX_COUNT) {
                mOpenCameraRetryCount = 0;
                showCameraFailedDialog();
            } else {
                mOpenCameraRetryCount++;
                Message msg = mHandler.obtainMessage(MSG_RETRY_RESUME_CAMERA);
                msg.arg1 = camId;
                mHandler.sendMessageDelayed(msg, OPEN_CAMERA_RETRY_TIME);
            }
        }
    }

    private void startCameraPreview() {
        try {
            mPublisher.startCamera();
            mOpenCameraRetryCount = 0;
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (mOpenCameraRetryCount >= OPEN_CAMERA_RETRY_MAX_COUNT) {
                mOpenCameraRetryCount = 0;
                showCameraFailedDialog();
            } else {
                mPublisher.stopCamera();
                mOpenCameraRetryCount++;
                mHandler.sendEmptyMessageDelayed(MSG_RETRY_START_CAMERA, OPEN_CAMERA_RETRY_TIME);
            }
        }
    }

    private void resumeLive() {
        try {
            muteRinger(true);

            mPublisher.setSendVideoOnly(false);
            // Cause the camera is closed when onPause(), and the encoding also stopped
            // So we have to start the camera preview, and resume encoding work
            mPublisher.startCameraAndResumeEnc();

            mLiveTimer.resumeCounting();
            startUpdateInteractInfo();

            mOpenCameraRetryCount = 0;
        } catch (NullPointerException e) {
            stopLive();

            mOpenCameraRetryCount = 0;
        } catch (IllegalStateException e) {
            stopLive();

            mOpenCameraRetryCount = 0;
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (mOpenCameraRetryCount >= OPEN_CAMERA_RETRY_MAX_COUNT) {
                mOpenCameraRetryCount = 0;
                showCameraFailedDialog();
            } else {
                mOpenCameraRetryCount++;
                mHandler.sendEmptyMessageDelayed(MSG_RETRY_RESUME_CAMERA, OPEN_CAMERA_RETRY_TIME);
            }
        }
    }

    private void getInitAudioMode() {
        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mPrevAudioMode = am.getRingerMode();
    }

    private void muteRinger(boolean mute) {
        if (getActivity() == null) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {
            return;
        }
        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        if (mute) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            am.setRingerMode(mPrevAudioMode);
        }
    }

    // Implementation of View.OnClickListener
    @Override
    public void onClick(View v) {
        // Clear the input edit text's focus
        mLiveInfoInput.clearFocus();

        handleDimScreen();

        switch (v.getId()) {
            case R.id.layout_user_info:
                showLogoutDialog();
                break;
            case R.id.layout_privacy_setting:
                startActivityForResult(new Intent(getActivity(), TimelineActivity.class),
                        REQUEST_LIVE_PRIVACY);
                break;
            case R.id.layout_4k_setting:
                if (m4KLiveSwitch != null) {
                    m4KLiveSwitch.toggle();
                }
                break;
            case R.id.btn_switch_camera:
                if (mPublisher.getCameraId() < 2) {
                    mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % 2);
                }
                break;
            case R.id.btn_select_camera:
                showDynamicCamLayout(true);
                break;
            case R.id.phone_cam:
                swapCamera(0);
                setDynamicCamBtnState(0);
                showDynamicCamLayout(false);
                mLive4KSettings.setVisibility(View.GONE);
                break;
            case R.id.mod_cam:
                swapCamera(MOTO_360_MOD_CAMERA);
                setDynamicCamBtnState(MOTO_360_MOD_CAMERA);
                showDynamicCamLayout(false);
                mLive4KSettings.setVisibility(View.VISIBLE);
                break;
            case R.id.dynamic_cam_btn:
                showDynamicCamLayout(false);
                break;
            case R.id.btn_exit:
                getActivity().finish();
                break;
            case R.id.btn_record_mute:
                mPublisher.setSendVideoOnly(!v.isSelected());
                v.setSelected(!v.isSelected());
                break;
            case R.id.btn_overflow:
                showPopupWindow(v);
                break;
            case R.id.btn_go_live:
                if (v.isSelected()) {
                    v.setSelected(false);
                    stopLive();
                } else {
                    mBtnSelectCamera.setEnabled(false);
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
            case R.id.btn_goto_fb:
                //postLiveVideo();
                // The Live video is posted by default and we no longer have the permission
                // to delete it. Instead we are taking the user to facebook where they can view
                // and/or delete the videp
                mResultLayout.setVisibility(View.GONE);
                refreshPreGoLiveUI();
                Util.jumpToFacebook(getActivity().getApplicationContext(),
                        mLiveInfoCacheBean.getUser());
                break;
            case R.id.btn_delete_live:
                deleteLiveVideo();
                break;
            case R.id.btn_positive:
            case R.id.btn_negative:
                if (mPostDialog != null) {
                    handleLivePostedDialogOnClick(v.getId());
                } else if (mLogoutDialog != null){
                    handleLogoutDialogOnClick(v.getId());
                }
                break;
            case R.id.btn_sphere_switch:
                switchSphereViewType();
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
        mHandler.removeMessages(MSG_CONNECT_LIVE_TIME_OUT);
        mHandler.sendEmptyMessage(MSG_LIVE_CONNECTED);
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
        handleNwException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleNwException(e);
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

        if (!mHandler.hasMessages(MSG_PUSH_LIVE_TIME_OUT)) {
            mHandler.sendEmptyMessageDelayed(MSG_PUSH_LIVE_TIME_OUT, PUSH_LIVE_TIME_OUT);
        }
    }

    @Override
    public void onNetworkResume() {
        Log.d(LOG_TAG, "Network resume");

        mHandler.removeMessages(MSG_PUSH_LIVE_TIME_OUT);
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onEncodeIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    private void startOpensourceLicense() {
        Intent intent = new Intent(getActivity(), AboutInfoActivity.class);
        startActivity(intent);
    }

    private void showPopupWindow(View v) {
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.about_menu_popup, null);
        mPopWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopWindow.setOutsideTouchable(true);
        Button mBt = (Button) contentView.findViewById(R.id.btn_about_menu_popupwindow);
        mBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOpensourceLicense();
                mPopWindow.dismiss();
            }
        });

        View flowButton = mBtnOpenSource;
        int[] location = new int[2];
        flowButton.getLocationOnScreen(location);

        mBt.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (isRtl()) {
            mPopWindow.showAtLocation(flowButton, Gravity.NO_GRAVITY,
                    location[0], location[1]);
        } else {
            mPopWindow.showAtLocation(flowButton, Gravity.NO_GRAVITY,
                    location[0] + flowButton.getMeasuredWidth() - mBt.getMeasuredWidth(), location[1]);
        }
    }

    public boolean isRtl() {
        return (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    public boolean isDuringLive() {
        return mLiveStatus == LiveStatus.LIVING;
    }

    // Begin, Lenovo, guzy2, IKSWN-71983, Stop live when user pressed Back key
    public void stopLiveByBack() {
        stopLive();
    }
    // End, Lenovo, guzy2, IKSWN-71983

    private void switchSphereViewType() {
        SphereViewRenderer.ViewType viewType = mSphereCameraView.switchViewType();
        switch (viewType) {
            case DEFAULT:
                //do nothing;
                break;
            case SPHERICAL:
                mBtnSphereSwitch.setImageResource(R.drawable.ic_view_spherical);
                break;
            case SPLITSCREEN:
                mBtnSphereSwitch.setImageResource(R.drawable.ic_view_equirectangular);
                break;
        }
    }

    private void setListenerToRootView() {
        final View rootView = getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mLiveStatus != LiveStatus.PRE_GO_LIVE
                        || mDynamicCamBtnLayout.getVisibility() == View.VISIBLE) {
                    return;
                }
                boolean mKeyboardUp = isKeyboardShown(rootView);
                if (mKeyboardUp) {
                    mLicenseLayout.setVisibility(View.GONE);
                    mGoLiveLayout.setVisibility(View.GONE);
                } else {
                    mLicenseLayout.setVisibility(View.VISIBLE);
                    mGoLiveLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isKeyboardShown(View rootView) {
        final int softKeyboardHeight = 100;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

    private void handleDimScreen() {
        if (isDuringLive()) {
            // Remove the existing delayed dim screen message
            mHandler.removeMessages(MSG_DIM_SCREEN_TIME_OUT);
            if (isScreenDimming()) {
                tryDimScreen(false);
            }

            // Re-send the message to dim screen
            mHandler.sendEmptyMessageDelayed(MSG_DIM_SCREEN_TIME_OUT, DIM_SCREEN_TIME_OUT);
        }
    }

    private boolean isScreenDimming() {
        return mIsScreenDimming;
    }

    private void tryDimScreen(boolean isDim) {
        if (getActivity() == null) {
            return;
        }

        mIsScreenDimming = isDim;

        Window window = getActivity().getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = isDim ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                    : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            window.setAttributes(lp);
        }
    }
}
