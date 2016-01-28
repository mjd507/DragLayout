package com.fighting.qqview;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.fighting.qqview.drag.DragLayout;
import com.fighting.qqview.drag.MyLinearLayout;
import com.fighting.qqview.utils.Cheeses;
import com.fighting.qqview.utils.Utils;

import java.util.Random;

/**
 * 描述：
 * 作者 mjd
 * 日期：2016/1/26 19:09
 */
public class MainActivity extends Activity {
    private ListView lvLeft;
    private MyLinearLayout mll;
    private ImageView ivHeader;
    private ListView lvMain;
    private DragLayout dragLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        dragLayout = (DragLayout) findViewById(R.id.dl);
        lvLeft = (ListView) findViewById(R.id.lv_left);
        mll = (MyLinearLayout) findViewById(R.id.mll);
        mll.setDraglayout(dragLayout);
        ivHeader = (ImageView) findViewById(R.id.iv_header);
        lvMain = (ListView) findViewById(R.id.lv_main);
    }

    private void initData() {
        lvLeft.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // android.R.layout.simple_list_item_1 中只有一个 TextView, 所以可以将其强转为 TextView
                // 修改其颜色
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                return view;
            }
        });
        lvMain.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.NAMES));

    }

    private void initListener() {
        dragLayout.setOnDragChangeListener(new DragLayout.OnDragChangeListener() {
            @Override
            public void onOpen() {
                Utils.showToast(getApplicationContext(), "onOpen");
                //面板打开时，左面板上的 listView 随机滑动到 0 到 50 间的某个位置
                lvLeft.smoothScrollToPosition(new Random().nextInt(50));
            }

            @Override
            public void onClose() {
                Utils.showToast(getApplicationContext(), "onClose");
                //关闭时，主面板上的头像有左右摆动的动画
                ValueAnimator anim = ValueAnimator.ofFloat(0f, 10f);
                //设置动画时长
                anim.setDuration(500);
                //给动画添加循环插值器，让动画连续执行多次
                anim.setInterpolator(new CycleInterpolator(4));
                //添加动画监听
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        //ValueAnimator 在 500ms 中，随着时间变化会计算出 0f->10f 间的数据
                        //通过 animation.getAnimatedValue() 可以获得这个值
                        float value = (Float) animation.getAnimatedValue();
                        //将这个值作用在 headView 上
                        ivHeader.setTranslationX(value);
                    }
                });
                //别忘记开启动画
                anim.start();
            }

            @Override
            public void onDraging(float percent) {
                Utils.showToast(getApplicationContext(), "onDraging:" + percent);
                //拖拽过程中，主面板上的头像透明度变化 1.0f->0.0f
                //percent 是 0.0f -> 1.0f =>1-percent -> 1.0f ->0.0f
                ivHeader.setAlpha(1 - percent);
            }
        });
    }

}
