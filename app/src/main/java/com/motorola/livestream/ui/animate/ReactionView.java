package com.motorola.livestream.ui.animate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.motorola.livestream.model.fb.Reaction;
import com.motorola.livestream.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ReactionView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = "ReactionView";

    private static final int MSG_ADD_REACTION_ANIM = 0x101;

    private final SurfaceHolder mSurfaceHolder;
    private final Paint mPaint;
    private final List<ReactionBean> mReactions;
    private final List<Reaction> mSrcReactions;

    //private GetReactionThread mGetReactionThread;
    private DrawThread mDrawThread;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_REACTION_ANIM:
                    addReactionBean();
                    // Delay 200 millis seconds to show next reaction icon
                    sendEmptyMessageDelayed(MSG_ADD_REACTION_ANIM, 200L);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    public ReactionView(Context context) {
        this(context, null);
    }

    public ReactionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReactionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setZOrderOnTop(true);
        // Set background as translucent
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mReactions = new ArrayList<>();
        mSrcReactions = new ArrayList<>();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mDrawThread != null) {
            mDrawThread.isRun = false;
            mDrawThread = null;
        }
    }

    public void addReactions(List<Reaction> reactionList) {
        mSrcReactions.addAll(reactionList);
        if (!mHandler.hasMessages(MSG_ADD_REACTION_ANIM)) {
            mHandler.sendEmptyMessage(MSG_ADD_REACTION_ANIM);
        }
    }

    private void addReactionBean() {
        if (mSrcReactions.size() > 0) {
            Reaction reaction = mSrcReactions.remove(0);
            ReactionBean reactionBean = new ReactionBean(ReactionView.this.getContext(),
                    reaction.getType().getReactionIcon(), ReactionView.this);

            synchronized (mReactions) {
                reactionBean.resume();
                mReactions.add(reactionBean);
                mReactions.notify();
            }
        }
    }

    public void start() {
        if (mDrawThread == null) {
            mDrawThread = new DrawThread();
            mDrawThread.start();
        }
    }

    public void clear() {
        mSrcReactions.clear();
        mReactions.clear();
    }

    public void stop() {
        if (mDrawThread != null) {
            for (ReactionBean reactionBean: mReactions) {
                reactionBean.pause();
            }
            mDrawThread.isRun = false;
            mDrawThread = null;

            // Be sure clear all reactions on the surface view when stop
            if (mSurfaceHolder != null) {
                Canvas canvas = mSurfaceHolder.lockCanvas();
                // Clear canvas first
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    class DrawThread extends Thread {
        boolean isRun = true;

        @Override
        public void run() {
            while (isRun) {
                Canvas canvas = null;
                try {
                    synchronized (mReactions) {
                        if (mReactions.size() == 0) {
                            Log.d(LOG_TAG, "Reaction list is empty, wait it be filled");
                            mReactions.wait();
                        }

                        canvas = mSurfaceHolder.lockCanvas();
                        // Clear canvas first
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                        ArrayList<ReactionBean> needRemoveList = new ArrayList<>();
                        for (ReactionBean reactionBean : mReactions) {
                            if (!reactionBean.isAnimEnd) {
                                reactionBean.draw(canvas, mPaint);
                            } else {
                                needRemoveList.add(reactionBean);
                            }
                        }
                        for (ReactionBean reactionBean : needRemoveList) {
                            mReactions.remove(reactionBean);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

                // Sleep 10 millis seconds to draw next frame
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
