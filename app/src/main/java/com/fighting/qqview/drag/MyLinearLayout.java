package com.fighting.qqview.drag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * 描述：
 * 作者 mjd
 * 日期：2016/1/27 14:02
 */
public class MyLinearLayout extends LinearLayout {

    private DragLayout mDragLayout;

    public MyLinearLayout(Context context) {
        super(context);
    }

    public MyLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mDragLayout != null && mDragLayout.getStatus() != DragLayout.Status.Close) {
            //不是关闭状态，直接拦截，不向下传递
            return true;
        } else {
            //是关闭状态, 按之前方法判断
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDragLayout != null && mDragLayout.getStatus() != DragLayout.Status.Close) {
            //不是关闭状态，直接拦截，不向下传递
            if (event.getAction() == MotionEvent.ACTION_UP) {
                //手指抬起时，关闭面板
                mDragLayout.close();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setDraglayout(DragLayout mDragLayout) {
        this.mDragLayout = mDragLayout;
    }
}
