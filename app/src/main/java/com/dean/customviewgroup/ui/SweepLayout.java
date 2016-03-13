package com.dean.customviewgroup.ui;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import static android.support.v4.widget.ViewDragHelper.Callback;
import static android.support.v4.widget.ViewDragHelper.STATE_DRAGGING;
import static android.support.v4.widget.ViewDragHelper.create;

/**
 * Created by Administrator on 2016/3/9.
 */
public class SweepLayout extends FrameLayout {

    private static final String TAG = "SweepLayout";
    private ViewDragHelper mViewDragHelper;
    /**
     * 内容View
     */
    private View mContentView;
    /**
     * 扩展View
     */
    private View mExtraView;
    /**
     * 可拖动的水平范围
     */
    private int mDragRange;
    /**
     * 可见区域的宽度
     */
    private int mContentWidth;
    /**
     * 可见区域的高度
     */
    private int mContentHeight;

    public Status getStatus() {
        return mStatus;
    }

    public enum Status {
        Close, Open, Dragging
    }

    private Status mStatus = Status.Close;

    public SweepLayout(Context context) {
        this(context, null);
    }

    public SweepLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mViewDragHelper = create(this, new ViewDragHelperCallback());
    }

    private OnStatusChangeListener mOnStatusChangeListener;

    public void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        mOnStatusChangeListener = onStatusChangeListener;
    }

    public interface OnStatusChangeListener {
        void onClose(SweepLayout view);

        void onOpen(SweepLayout view);

        void onDragging(SweepLayout view);

        void onPrepareOpen(SweepLayout view);

        void onPrepareClose(SweepLayout view);
    }

    private void dispatchStatusEvent(Status prevStatus, Status curStatus) {
        if (prevStatus != curStatus && mOnStatusChangeListener != null) {
            if (curStatus == Status.Open) {
                mOnStatusChangeListener.onOpen(this);
            } else if (curStatus == Status.Close) {
                mOnStatusChangeListener.onClose(this);
            } else if (prevStatus == Status.Close && curStatus == Status.Dragging) {
                mOnStatusChangeListener.onPrepareOpen(this);
            } else if (prevStatus == Status.Open && curStatus == Status.Dragging) {
                mOnStatusChangeListener.onPrepareClose(this);
            } else if (curStatus == Status.Dragging) {
                mOnStatusChangeListener.onDragging(this);
            }
        }
    }

    private void updateStatus() {
        int left = mContentView.getLeft();
        if (left == 0) {
            mStatus = Status.Close;
        } else if (left == -mDragRange) {
            mStatus = Status.Open;
        } else {
            mStatus = Status.Dragging;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void open() {
        open(true);
    }

    private void open(boolean smooth) {
        int finalLeft = -mDragRange;
        slideView(smooth, finalLeft);
    }

    public void close() {
        close(true);
    }

    private void close(boolean smooth) {
        int finalLeft = 0;
        slideView(smooth, finalLeft);
    }

    //滑动mContentView到指定的左边距
    private void slideView(boolean smooth, int finalLeft) {
        if (smooth) {
            if (mViewDragHelper.smoothSlideViewTo(mContentView, finalLeft, getPaddingTop())) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            //2个View都要布局
            mContentView
                    .layout(finalLeft, getPaddingTop(), finalLeft + mContentWidth, mContentHeight);
            mExtraView
                    .layout(finalLeft + mContentWidth, getPaddingTop(), finalLeft + mContentWidth + mDragRange, mContentHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //让扩展View藏在正文View的右边
        mExtraView.offsetLeftAndRight(mContentWidth);
        //bringChildToFront(mContentView);
    }

    private class ViewDragHelperCallback extends Callback{
        @Override
        public boolean tryCaptureView(android.view.View child, int pointerId) {
            return true;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (mViewDragHelper.getViewDragState() == STATE_DRAGGING && Math
                    .abs(dx) > 10) {
                requestDisallowInterceptTouchEvent(true);//手指在左右滑动时禁止父控件拦截事件，让我自由滑动
            }
            if (child == mContentView) {
                left = left > 0 ? 0 : left;
                left = left < -mDragRange ? -mDragRange : left;
            } else {
                left = left > mContentWidth ? mContentWidth : left;
                left = left < mContentWidth - mDragRange ? mContentWidth - mDragRange : left;
            }
            return left;
        }


        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return getPaddingTop();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (changedView == mContentView) {
                mExtraView.offsetLeftAndRight(dx);
            } else {
                mContentView.offsetLeftAndRight(dx);
            }
           ViewCompat.postInvalidateOnAnimation(SweepLayout.this);
            Status prevStatus = getStatus();
            updateStatus();
            dispatchStatusEvent(prevStatus, getStatus());
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            //该方法不用真正限制拖拽的范围，这里大于0即可
            //它的返回值仅仅决定当参数child包含有点击事件的子child时child是否可以拖拽以及用于计算动画执行的时长
            return  mDragRange;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (xvel == 0 && mContentView.getLeft() < -mDragRange / 2f) {
                open();
            } else if (xvel < 0) {
                open();
            } else {
                close();
            }
            requestDisallowInterceptTouchEvent(false);
        }
    };


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev) ;//& mGestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getChildCount() != 2) {
            throw new IllegalStateException("有且仅有2个子View");
        }

        mContentView = getChildAt(0);
        mExtraView = getChildAt(1);
        //记录可显示区域的宽度和高度
        mContentWidth = w;
        mContentHeight = h;
        //获取可拖动的X轴范围，即ExtraView的宽度
        mDragRange = mExtraView.getMeasuredWidth();
    }
}
