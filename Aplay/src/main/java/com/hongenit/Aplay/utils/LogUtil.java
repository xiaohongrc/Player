package com.hongenit.Aplay.utils;


import android.util.Log;

/**
 * Created by hongenit on 2018/6/18.
 * desc:
 */

public class LogUtil {

    private static final boolean willLog = true;

    public static void i(Object object, String message) {
        if (willLog) {
            Log.i(object.getClass().getSimpleName(), message);
        }

    }

    public static void d(Object object, String message) {
        if (willLog) {
            Log.d(object.getClass().getSimpleName(), message);
        }

    }

    public static void w(Object object, String message) {
        if (willLog) {
            Log.w(object.getClass().getSimpleName(), message);
        }
    }

    public static void e(Object object, String message) {
        if (willLog) {
            Log.e(object.getClass().getSimpleName(), message);
        }

    }


}
