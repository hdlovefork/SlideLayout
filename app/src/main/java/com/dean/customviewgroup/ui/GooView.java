package com.dean.customviewgroup.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.dean.customviewgroup.R;
import com.dean.customviewgroup.utils.GeometryUtil;


/**
 * Created by Administrator on 2016/3/10.
 */
public class GooView extends View {
    //原始点的默认半径
    static final float RADIUS_ORIGIN_CIRCLE = 12f;
    //拖拽点的默认半径
    static final float RADIUS_DRAG_CIRCLE = 12f;
    //允许拖拽的默认距离
    static final float DRAG_DISTANCE = 100f;
    //控制拖拽圆是否绘制
    static final float INVALID_VALUE = Integer.MAX_VALUE;
    private static final String TAG = "GooView";
    //默认的文字大小、颜色、样式
    private static final int TEXT_DEFAULT_SIZE = 12;
    private static final int TEXT_DEFAULT_COLOR = Color.WHITE;
    private static final int TEXT_DEFAULT_STYLE = Typeface.BOLD;
    //用于在整个屏幕中添加一个消失动画
    private final WindowManager mWm;
    private final WindowManager.LayoutParams mParams;

    //自身宽高
    private int mWidth;
    private int mHeight;

    /**
     * 绘制自身的形状的画笔
     */
    private Paint mCirclePaint;

    /**
     * 原始点半径大小
     */
    private float mOriginRadius = RADIUS_ORIGIN_CIRCLE;
    /**
     * 拖拽点半径大小
     */
    private float mDragRadius = RADIUS_DRAG_CIRCLE;
    /**
     * 原始点圆心坐标
     */
    private PointF mOriginCenterPt;
    /**
     * 拖拽点圆心坐标
     */
    private PointF mDragCenterPt;
    /**
     * 每次重绘时刷新的范围
     */
    private Rect mInvalidateRect;
    /**
     * 保存绘制路径，用于绘制贝塞尔曲线
     */
    private Path mPath;
    /**
     * 用于绘制贝塞尔曲线的控制点，默认是2个圆心连线的中心
     */
    private PointF mControlPt;
    /**
     * 允许拖拽的最大距离
     */
    private float mDragDistance = DRAG_DISTANCE;
    /**
     * 指示拖拽是否已经超出范围
     */
    private boolean mIsOutOfRange;
    /**
     * 指示是否拖拽的对象是否已经销毁
     */
    private boolean mIsRemove;
    /**
     * 绘制文本的画笔
     */
    private TextPaint mTextPaint;
    /**
     * 显示的文本
     */
    private String mText;
    private boolean mDragging;
    private int mCurTextColor = Color.WHITE;

    public GooView(Context context) {
        this(context, null);
    }

    public GooView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(Color.RED);
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mOriginCenterPt = new PointF();
        mDragCenterPt = new PointF(INVALID_VALUE, INVALID_VALUE);
        mInvalidateRect = new Rect();
        mPath = new Path();
        mWm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams();
        mParams.format = PixelFormat.TRANSLUCENT;

        /*
         * Look the appearance up without checking first if it exists because
         * almost every TextView has one and it greatly simplifies the logic
         * to be able to parse the appearance first and then let specific tags
         * for this View override it.
         */
        ColorStateList textColor = null;
        int textSize = TEXT_DEFAULT_SIZE;
        String fontFamily = null;
        int styleIndex = TEXT_DEFAULT_STYLE;

        TypedArray appearance = context.obtainStyledAttributes(
                attrs, R.styleable.TextAppearance, 0, 0);
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int index = appearance.getIndex(i);
                if (index < 0 && index >= R.styleable.TextAppearance.length) {
                    continue;
                }
                int attr = R.styleable.TextAppearance[index];
                switch (attr) {
                    case android.R.attr.textColor:
                        textColor = appearance.getColorStateList(index);
                        break;
                    case android.R.attr.textSize:
                        textSize = appearance.getDimensionPixelSize(index, textSize);
                        break;
                    case android.R.attr.textStyle:
                        styleIndex = appearance.getInt(index, -1);
                        break;
                }
            }
            setTextColor(textColor != null ? textColor : ColorStateList
                    .valueOf(TEXT_DEFAULT_COLOR));
            setTypefaceFromAttrs(fontFamily, styleIndex);
            mTextPaint.setTextSize(textSize);
            appearance.recycle();
        }

    }

    /**
     * 获取文本颜色
     *
     * @return
     */
    public int getTextColor() {
        return mCurTextColor;
    }

    /**
     * 设置文本颜色
     *
     * @param textColor
     */
    public void setTextColor(int textColor) {
        mTextColor = ColorStateList.valueOf(textColor);
        updateTextColors();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }

        mTextColor = colors;
        updateTextColors();
    }

    private void updateTextColors() {
        int color = mTextColor.getColorForState(getDrawableState(), 0);
        if (color != mCurTextColor) {
            mCurTextColor = color;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * 获取文本大小
     *
     * @return
     */
    public int getTextSize() {
        return mTextSize;
    }

    /**
     * 设置文本大小
     *
     * @param textSize
     */
    public void setTextSize(int textSize) {
        mTextSize = textSize;
        mTextPaint.setTextSize(textSize);
        ViewCompat.postInvalidateOnAnimation(this);
    }


    /**
     * 显示的文本颜色
     */
    private ColorStateList mTextColor;
    /**
     * 显示的文本大小
     */
    private int mTextSize;

    /**
     * 当拖拽出有效范围时播放的帧动画资源ID
     *
     * @param animationResId
     */
    public void setRemoveAnimation(int animationResId) {
        mRemoveAnimationResId = animationResId;
    }

    /**
     * 获取显示的文本
     *
     * @return
     */
    public String getText() {
        return mText;
    }

    /**
     * 设置显示的文本
     *
     * @param text
     */
    public void setText(String text) {
        mText = text;
        ViewCompat.postInvalidateOnAnimation(this);
    }


    /**
     * 当拖拽出有效范围时播放的帧动画资源ID
     */
    private int mRemoveAnimationResId;


    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void setTypefaceFromAttrs(String familyName, int styleIndex) {
        Typeface tf = null;
        if (familyName != null) {
            tf = Typeface.create(familyName, styleIndex);
            if (tf != null) {
                setTypeface(tf);
                return;
            }
        }
        setTypeface(tf, styleIndex);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //确保绘制的原始圆能完整的显示，所以这里取宽高的最小值，然后除以2作为半径
        mDragRadius = mOriginRadius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsRemove) {
            //已经被销毁了无须绘制
            return;
        }
        if (mIsOutOfRange) {
            //已经被拖出有效范围，只绘制拖拽圆
            canvas.drawCircle(mDragCenterPt.x, mDragCenterPt.y, mDragRadius, mCirclePaint);
            return;
        }
        //计算原始圆新的半径大小（原始点的半径随着拖拽的距离越远越小）
        float tmpRadius = getRadius();
        //绘制原始圆
        canvas.drawCircle(mOriginCenterPt.x, mOriginCenterPt.y, tmpRadius, mCirclePaint);

        if (mDragCenterPt.equals(INVALID_VALUE, INVALID_VALUE)) {
            drawText(canvas);//绘制文本
            return;
        }
        //计算用于绘制贝塞尔路径的4个点坐标，每个圆2个，这2个点的连线会通过圆心
        float xOffset = mOriginCenterPt.x - mDragCenterPt.x;
        float yOffset = mOriginCenterPt.y - mDragCenterPt.y;
        double lineK = yOffset / xOffset;
        PointF[] originIntersect = GeometryUtil
                .getIntersectionPoints(mOriginCenterPt, tmpRadius, lineK);
        PointF[] dragIntersect = GeometryUtil
                .getIntersectionPoints(mDragCenterPt, mDragRadius, lineK);

        //计算控制点坐标
        mControlPt = GeometryUtil.getMiddlePoint(mOriginCenterPt, mDragCenterPt);
        //绘制含贝塞尔曲线的路径
        mPath.reset();
        mPath.moveTo(originIntersect[0].x, originIntersect[0].y);
        mPath.quadTo(mControlPt.x, mControlPt.y, dragIntersect[0].x, dragIntersect[0].y);
        mPath.lineTo(dragIntersect[1].x, dragIntersect[1].y);
        mPath.quadTo(mControlPt.x, mControlPt.y, originIntersect[1].x, originIntersect[1].y);
        mPath.close();

        canvas.drawPath(mPath, mCirclePaint);

        //绘制拖拽圆
        canvas.drawCircle(mDragCenterPt.x, mDragCenterPt.y, mDragRadius, mCirclePaint);
    }

    /**
     * 绘制文本
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        //没有拖拽时绘制文本
        if (!TextUtils.isEmpty(mText)) {
            // 计算Baseline绘制的起点X轴坐标 ，计算方式：画布宽度的一半，因为画笔已经设置了文本居中对齐
            mTextPaint.setColor(mCurTextColor);
            mTextPaint.drawableState = getDrawableState();
            int baseX = mWidth / 2;
            // 计算Baseline绘制的Y坐标 ，计算方式：画布高度的一半 - 文字总高度的一半
            int baseY = (int) ((mHeight / 2) - ((mTextPaint.descent() + mTextPaint
                    .ascent()) / 2));

            // 居中画一个文字
            canvas.drawText(mText, baseX, baseY, mTextPaint);
        }
    }


    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mTextColor != null && mTextColor.isStateful()) {
            updateTextColors();
        }
    }

    /**
     * 根据拖拽的距离计算原始点半径大小，拖拽距离越远原始点越小，如果没有拖拽时返回原始圆设置的半径大小
     *
     * @return 返回原始点当前半径大小
     */
    private float getRadius() {
        if (mDragCenterPt.equals(INVALID_VALUE, INVALID_VALUE)) {
            //没有被拖拽直接返回原始圆的半径
            return mOriginRadius;
        }
        //正在被拖拽，根据当前拖拽距离与允许的拖拽范围的比例计算出原始圆新的半径大小
        float distance = GeometryUtil
                .getDistanceBetween2Points(mOriginCenterPt, mDragCenterPt);
        distance = Math.min(distance, mDragDistance);
        float fraction = distance / mDragDistance;//计算拖拽距离与拖拽有效范围的比例
        return evaluate(fraction, mOriginRadius, mOriginRadius * 0.2f);
    }

    /**
     * 根据fraction的比例（0~1之间的小数值）与总距离计算当前值
     *
     * @param fraction
     * @param startValue
     * @param endValue
     * @return
     */
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        //根据宽高计算出自身圆心位置
        mOriginCenterPt.set(mWidth / 2f, mHeight / 2f);
        //mDragCenterPt.set(-50, -50);
        //在父控件中可以看到这个View
        supportDrawInParent();
        //设置刷新时的无效区域为整个屏幕
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int[] pt = new int[2];
        this.getLocationOnScreen(pt);//获取自己与屏幕的左上距离
        mInvalidateRect.set(-pt[0], -pt[1], screenWidth, screenHeight);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        mOnStatusChangeListener = onStatusChangeListener;
    }

    private OnStatusChangeListener mOnStatusChangeListener;

    /**
     * 以初始化状态绘制圆和文本内容
     */
    public void show() {
        mIsRemove = false;
        mIsOutOfRange = false;
        mDragCenterPt.set(INVALID_VALUE, INVALID_VALUE);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    /**
     * 隐藏圆和文本内容让他变得不可见，但它实际上占用了空间这不同于View.INVISIBLE 和 View.GONE
     */
    public void hide() {
        mIsRemove = true;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public interface OnStatusChangeListener {
        /**
         * 没有拖出有效范围已经还原到原始位置
         *
         * @param gooView
         * @param isOutOfRange 是否已经超过有效拖拽范围
         */
        void onRestore(GooView gooView, boolean isOutOfRange);

        /**
         * 气泡被拖走，已经消失了
         *
         * @param gooView
         */
        void onRemove(GooView gooView);

        /**
         * 正在被拖拽
         *
         * @param gooView
         * @param x       相对于屏幕的X
         * @param y       相对于屏幕的Y
         */
        void onDragging(GooView gooView, float x, float y);

        /**
         * 点击事件
         *
         * @param gooView
         */
        void onClick(GooView gooView);
    }

    //更新拖拽点的位置以及引发事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float distance;
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                //已经销毁不再拦截事件
                if (mIsRemove) {
                    return false;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                //刚刚按下时拖拽圆没有超出有效范围
                mIsOutOfRange = false;
                mDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                mDragging = true;
                Log.d(TAG, "x:" + event.getX() + " y:" + event.getY());
                //更新拖拽圆的位置
                updateDrag(event.getX(), event.getY());
                //计算原始圆与拖拽圆圆心之间的距离
                distance = GeometryUtil
                        .getDistanceBetween2Points(mOriginCenterPt, mDragCenterPt);
                if (distance > mDragDistance) {
                    //两圆之间的距离超出了指定的范围
                    mIsOutOfRange = true;
                }
                if (mOnStatusChangeListener != null) {
                    mOnStatusChangeListener.onDragging(this, event.getRawX(), event.getRawY());
                }
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (mIsOutOfRange) {
                    //之前拖拽超出了有效范围
                    distance = GeometryUtil
                            .getDistanceBetween2Points(mOriginCenterPt, mDragCenterPt);
                    //再次判断枪手时有没有超出范围，如果超出范围则销毁它，否则恢复到原始圆的位置
                    if (distance > mDragDistance) {
                        //松手时没有放回去需要销毁它
                        mIsRemove = true;
                        ViewCompat
                                .postInvalidateOnAnimation(this, mInvalidateRect.left, mInvalidateRect.top, mInvalidateRect.right, mInvalidateRect.bottom);
                        if (mOnStatusChangeListener != null) {
                            mOnStatusChangeListener.onRemove(this);
                        }
                        //播放消失动画
                        if (mRemoveAnimationResId != 0) {
                            playRemoveAnimation();
                        }
                    } else {//松手时放回去了，还原
                        updateDrag(INVALID_VALUE, INVALID_VALUE);
                        if (mOnStatusChangeListener != null) {
                            mOnStatusChangeListener.onRestore(this, mIsOutOfRange);
                        }
                        mIsOutOfRange = false;
                    }
                } else if (mDragging) {
                    //松手时拖拽圆超出出指定的范围，弹回去
                    playRestoreAnimation();
                    if (mOnStatusChangeListener != null) {
                        mOnStatusChangeListener.onRestore(this, mIsOutOfRange);
                    }
                } else {
                    if (mOnStatusChangeListener != null) {
                        mOnStatusChangeListener.onClick(this);
                    }
                }
                break;
        }
        return true;
    }

    /**
     * 播放拖拽圆还原到原始圆位置的弹性动画
     */
    private void playRestoreAnimation() {
        //保存当前拖拽圆的起点，作为动画的起始位置
        final PointF tempDragCenter = new PointF(mDragCenterPt.x, mDragCenterPt.y);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1.0f);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //计算当前这一帧DragView的圆心与OriginView的圆心之间的距离，逐帧绘制
                float percent = animation.getAnimatedFraction();
                //percent在0~1.0之间，这里计算新的拖拽圆的圆心位置
                PointF p = GeometryUtil
                        .getPointByPercent(tempDragCenter, mOriginCenterPt, percent);
                updateDrag(p.x, p.y);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //动画结束时将拖拽圆隐藏起来
                updateDrag(INVALID_VALUE, INVALID_VALUE);
            }
        });
        valueAnimator.setDuration(300);
        //拖拽圆到达原始圆位置后做个张力动画，弹一下的效果
        valueAnimator.setInterpolator(new OvershootInterpolator(4));
        valueAnimator.start();
    }

    /**
     * 播放销毁时的动画
     */
    private void playRemoveAnimation() {
        ImageView view = new ImageView(getContext());
        view.setBackgroundResource(mRemoveAnimationResId);
        AnimationDrawable animationDrawable = (AnimationDrawable) view.getBackground();
        //ImageView默认对齐于屏幕左上角，如果不指定下面代码的话，它将默认显示在屏幕的正中间位置，并且坐标系统的0，0点也是在屏幕正中间
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        //        mParams.x= (int) (mDragCenterPt.x-animationDrawable.getIntrinsicWidth()/2);
        //        mParams.y= (int) (mDragCenterPt.y-animationDrawable.getIntrinsicHeight()/2);
        //获得通知栏高度
        Rect rect = new Rect();
        this.getWindowVisibleDisplayFrame(rect);
        //获得这个View在屏幕中的位置，包括通知栏在内
        int[] ps = new int[2];
        this.getLocationOnScreen(ps);
        //计算动画在屏幕中的位置
        mParams.x = (int) (ps[0] + mDragCenterPt.x - animationDrawable.getIntrinsicWidth() / 2);
        mParams.y = (int) (ps[1] - rect.top + mDragCenterPt.y - animationDrawable
                .getIntrinsicHeight() / 2);
        //动画ImageView大小默认为图片的大小
        mParams.width = animationDrawable.getIntrinsicWidth();
        mParams.height = animationDrawable.getIntrinsicHeight();
        mWm.addView(view, mParams);
        //播放动画
        animationDrawable.start();
        //监听动画完成，从WindowManager中移除这个ImageView
        listenAnimComplete(view, animationDrawable);
    }

    /**
     * 销毁动画结束后从WindowManager中删除这个ImageView
     *
     * @param view
     * @param animationDrawable
     */
    private void listenAnimComplete(final ImageView view, AnimationDrawable animationDrawable) {
        //计算动画总时长，每帧之和
        int duration = 0;
        for (int i = 0; i < animationDrawable.getNumberOfFrames(); i++) {
            duration += animationDrawable.getDuration(i);
        }
        //动画执行完成后删除这个ImageView
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                //从WindowManager中删除ImageView
                mWm.removeView(view);
            }
        }, duration);
    }

    /**
     * 更新拖拽点的位置并重绘
     *
     * @param x
     * @param y
     */
    private void updateDrag(float x, float y) {
        mDragCenterPt.set(x, y);
        ViewCompat
                .postInvalidateOnAnimation(this, mInvalidateRect.left, mInvalidateRect.top, mInvalidateRect.right, mInvalidateRect.bottom);
    }

    /**
     * 让父控件允许我绘制的内容超出他的范围
     */
    private void supportDrawInParent() {
        View parent = (View) getParent();
        while (parent != null) {
            if (!(parent instanceof ViewGroup)) {
                break;
            }
            ViewGroup p = (ViewGroup) parent;
            p.setClipChildren(false);
            p.setClipToPadding(false);
            if (!(p.getParent() instanceof View)) {
                break;
            }
            parent = (View) p.getParent();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (getVisibility() != visibility) {
            //变得可见说明程序员想要他显示出来，所以设置mIsRemove为false
            if (visibility == VISIBLE) {
                mIsRemove = false;
            }
        }
        super.setVisibility(visibility);
    }

    public static class SimpleOnStatusChangeListener implements OnStatusChangeListener {

        @Override
        public void onRestore(GooView gooView, boolean isOutOfRange) {

        }

        @Override
        public void onRemove(GooView gooView) {

        }

        @Override
        public void onDragging(GooView gooView, float x, float y) {

        }

        @Override
        public void onClick(GooView gooView) {

        }
    }
}
