package com.fighting.qqview.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 描述：
 * 作者 mjd
 * 日期：2016/1/27 14:20
 */
public class Utils {

    public static Toast mToast;

    public static void showToast(Context mContext, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(msg);
        mToast.show();
    }

}
