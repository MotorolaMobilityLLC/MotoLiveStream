package com.motorola.livestream.ui.animate;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.SparseArray;

import java.util.Random;

public class ReactionBean {

    private static final SparseArray<Bitmap> mReactionBitmapCache = new SparseArray<>();
    private static int lastReactionHeight = 0;

    public Point mPoint;
    private ValueAnimator mMoveAnim;
    private Bitmap mBitmap;
    private Matrix mMatrix = new Matrix();
    public boolean isAnimEnd = false;

    public ReactionBean(Context context, int resId, ReactionView reactionView) {
        mBitmap = mReactionBitmapCache.get(resId);
        if (mBitmap == null) {
            mBitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            mReactionBitmapCache.put(resId, mBitmap);
        }

        int bitmapHeight = mBitmap.getHeight();
        int randomHeight = 0;
        while (true) {
            randomHeight = new Random().nextInt((reactionView.getHeight() - bitmapHeight));
            // Be sure the adjacent reaction won't cover each other
            if (Math.abs(randomHeight - lastReactionHeight) > bitmapHeight) {
                break;
            }
        }

        lastReactionHeight = randomHeight;
        init(new Point(reactionView.getWidth(), randomHeight),
                new Point(-mBitmap.getWidth(), randomHeight));
    }

    public void resume() {
        if (mMoveAnim != null && mMoveAnim.isPaused()) {
            mMoveAnim.resume();
        }
    }

    public void pause() {
        if (mMoveAnim != null && mMoveAnim.isRunning()) {
            mMoveAnim.pause();
        }
    }

    public void draw(Canvas canvas, Paint p) {
        if (mBitmap != null) {
            mMatrix.reset();
            mMatrix.postTranslate(mPoint.x, mPoint.y);
            canvas.drawBitmap(mBitmap, mMatrix, p);
        } else {
            isAnimEnd = true;
        }
    }

    private void init(Point startPoint, Point endPoint) {
        mMoveAnim = ValueAnimator.ofObject(new MoveEvaluator(), startPoint, endPoint);
        mMoveAnim.setDuration(3000L);
        mMoveAnim.addUpdateListener((ValueAnimator animation) -> {
            mPoint = (Point) animation.getAnimatedValue();
        });
        mMoveAnim.addListener(new ValueAnimator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                isAnimEnd = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimEnd = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimEnd = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                isAnimEnd = true;
            }
        });
        mMoveAnim.start();
    }

    private class MoveEvaluator implements TypeEvaluator<Point> {

        @Override
        public Point evaluate(float fraction, Point startValue, Point endValue) {
            int x = (int) ((1 - fraction) * (1 - fraction) * startValue.x + fraction * fraction * endValue.x);
            return new Point(x, startValue.y);
        }
    }
}
