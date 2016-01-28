package com.fighting.qqview.drag;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 描述：
 * 作者 mjd
 * 日期：2016/1/26 19:10
 */
public class DragLayout extends FrameLayout {

    private static final String TAG = "DragLayout";
    private ViewDragHelper viewDragHelper;
    private View leftContent;
    private View mainContent;
    private int viewWidth;
    private int viewHeight;
    private int range;


    public DragLayout(Context context) {
        //代码创建时调用
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        //布局在 xml 中，实例化时调用
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 在这里初始化
        // 1.forParent 父类容器 2.sensitivity 敏感度，越大越敏感， 1.0f 是默认值 3.Callback 回调事件
        //1. 通静态方法创建拖拽辅助类
        viewDragHelper = ViewDragHelper.create(this, 1.0f, mCallback);
    }

    //2. 转交触摸事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //return super.onInterceptTouchEvent(ev);
        //由 ViewDragHelper 判断是否拦截
        return viewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            viewDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //3. 处理回调事件
    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {

        //返回值决定了 child 是否可以被拖拽
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            //child 被用户拖拽的孩子 pointerId 多点触摸的手指 id
            return true;
        }

        //返回拖拽的范围，返回一个大于零的值，计算动画执行的时长，水平方向是否可以被滑开
        @Override
        public int getViewHorizontalDragRange(View child) {
            //computeSettleDuration 计算动画执行的时长
            //checkTouchSlop 检查是否可以被滑动（没有孩子处理触摸事件， 最后返回给 DragLayout 处理）
            return range;
        }

        //修正子 view 水平方向上的位置，此时还没有真正的移动，返回值决定 view 将移动到的位置
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            //child 被用户拖拽的孩子 left 建议移动到的位置 dx 新的位置与旧的位置的差值
            int oldLeft = mainContent.getLeft();
            Log.e(TAG, "clamp: left:" + left + " oldLeft:" + oldLeft + " dx:" + dx);
            if (child == mainContent) {
                left = fixLeft(left);
            }
            return left;
        }

        // 当控件位置变化时调用，可以做伴随动画，状态更新，事件回调
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            // left 最新的水平位置 dx 刚刚发生的水平变化量
            Log.e(TAG, "onViewPositionChanged: left:" + left + " dx:" + dx);
            if (changedView == leftContent) {
                // 如果滑动的是左面板
                // 1. 放回到原来的位置
                leftContent.layout(0, 0, viewWidth, viewHeight);
                // 2. 把变化量传递给主面板, 主面板旧的值+变化量
                int newLeft = mainContent.getLeft() + dx;
                // 需要修正左边值
                newLeft = fixLeft(newLeft);
                mainContent.layout(newLeft, 0, newLeft + viewWidth, viewHeight);
            }
            dispatchDragEvent();
            // offsetLeftAndRight 在低版本中没有重绘界面，手动调用重绘
            invalidate();
        }

        //5. 决定松手后要做的事件，结束动画
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            //releasedChild 被释放的孩子 xvel 水平方向的速度, 向左为-，向右为+
            Log.e(TAG, "onViewReleased: xvel:" + xvel);
            //考虑开启的情况，其它情况则关闭的情况
            if (xvel == 0 && mainContent.getLeft() > range * 0.5f) {
                //在允许滑动的范围的中轴线右边，则打开
                open();
            } else if (xvel > 0) {
                //速度向右时，则打开
                open();
            } else {
                //关闭
                close();
            }
        }

    };

    private void dispatchDragEvent() {
        //0.0f->1.0f 获取动画的百分比，主面板左边的位置引起的一系列变化
        float percent = mainContent.getLeft() * 1.0f / range;
        Log.e(TAG, "dispatchDragEvent: percent:" + percent);

        Status lastStatus = status;
        //更新状态，通过动画百分比判断
        if (percent == 0) {
            status = Status.Close;
        } else if (percent == 1) {
            status = Status.Open;
        } else {
            status = Status.Draging;
        }
        if (onDragChangeListener != null) {
            //调用频率高，直接调用
            onDragChangeListener.onDraging(percent);
        }

        if (lastStatus != status && onDragChangeListener != null) {
            if (status == Status.Open) {
                //最新状态是 open，说明刚才不是 open，则需要调用一下 onOpen 方法
                onDragChangeListener.onOpen();
            } else if (status == Status.Close) {
                //最新状态是 close，说明刚才不是 close，则需要调用一下 onClose 方法
                onDragChangeListener.onClose();
            }
        }

        //左面板：缩放动画，平移动画，透明度动画
        //0.0f ->1.0f percent*0.5f => 0.0f -> 0.5f
        //寻找规律->拷贝 FloatEvaluator.java 中的估值方法
        //percent*0.5f + 0.5f => 0.5f -> 1.0f
        //percent*(1.0f -0.6f)+0.6f => 0.6f -> 1.0f => start + percent(end - start)

        //1. 缩放动画, 从 50%->100%
        leftContent.setScaleX(evaluate(percent, 0.5f, 1.0f));
        leftContent.setScaleY(evaluate(percent, 0.5f, 1.0f));

        //2. 平移动画, 从宽度一半在屏幕外->全部移到屏幕内
        leftContent.setTranslationX(evaluate(percent, -viewWidth * 0.5f, 0f));

        //3. 透明度动画, 从 20%->100%
        leftContent.setAlpha(evaluate(percent, 0.2f, 1.0f));

        //主面板：缩放动画, 从 100%->80%
        mainContent.setScaleY(evaluate(percent, 1.0f, 0.8f));

        //背景亮度变化,PorterDuff.Mode.SRC_OVER 叠加模式，直接叠加在上面
//        getBackground().setColorFilter((Integer)  evaluateColor(percent, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    //源码 ArgbEvaluator.java 中拷贝的估值方法
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        //api18 以上的代码才有透明度的过滤
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (startA + (int) (fraction * (endA - startA)) << 24) |
                (startR + (int) (fraction * (endR - startR)) << 16) |
                (startG + (int) (fraction * (endG - startG)) << 8) |
                (startB + (int) (fraction * (endB - startB)));
    }


    //源码 FloatEvaluator.java 中拷贝的估值方法
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    protected void open() {
        open(true);
    }

    protected void open(boolean isSmooth) {
        int finalLeft = range;
        stateAnim(isSmooth, finalLeft);
    }

    protected void close() {
        close(true);
    }

    protected void close(boolean isSmooth) {
        int finalLeft = 0;
        stateAnim(isSmooth, finalLeft);
    }

    private void stateAnim(boolean isSmooth, int finalLeft) {
        if (isSmooth) {
            //触发一个平滑动画
            if (viewDragHelper.smoothSlideViewTo(mainContent, finalLeft, 0)) {
                //invalidate(); 可能会漏帧
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            //直接跳转
            mainContent.layout(finalLeft, 0, finalLeft + viewWidth, viewHeight);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        //调用完后会调用 draw()
        if (viewDragHelper.continueSettling(true)) {
            //参数传入 true，表示延迟画下一帧
            //mViewDragHelper.continueSettling(true)
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * 修正左边的位置，限定拖拽范围在 0 到 range 间变化
     */
    private int fixLeft(int left) {
        if (left < 0) {
            left = 0;
        } else if (left > range) {
            left = range;
        }
        return left;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //增强代码的健壮性
        if (getChildCount() < 2) {
            //必须有两个子 view
            throw new IllegalStateException("Your viewGroup must have two children.");
        }
        if (!(getChildAt(0) instanceof ViewGroup) || !(getChildAt(1) instanceof ViewGroup)) {
            //子 view 必须是 viewGroup 的子类
            throw new IllegalStateException("The child must an instance of viewGroup.");
        }
        leftContent = getChildAt(0);
        mainContent = getChildAt(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        range = (int) (viewWidth * 0.6f);
        Log.e(TAG, "viewWidth = " + viewWidth + ", viewHeight = " + viewHeight + ", range = " + range);
    }

    //默认状态为关闭
    private Status status = Status.Close;

    //提供 get() 方法
    public Status getStatus() {
        return status;
    }

    //状态的枚举值，有三种状态，打开，关闭，拖拽中
    public enum Status {
        Open, Close, Draging;
    }

    //接收外界注册的接口类，以便回调接口方法
    private OnDragChangeListener onDragChangeListener;

    //提供 set() 方法，让外界注册监听接口类
    public void setOnDragChangeListener(OnDragChangeListener onDragChangeListener) {
        this.onDragChangeListener = onDragChangeListener;
    }

    //模仿 View 的 OnClickListener 的写法，定义一个内部的公开的接口
    public interface OnDragChangeListener {
        /**
         * 打开时调用
         */
        void onOpen();

        /**
         * 关闭时调用
         */
        void onClose();

        /**
         * 拖拽中调用
         *
         * @param percent 当前拖拽的百分比
         */
        void onDraging(float percent);
    }
}
