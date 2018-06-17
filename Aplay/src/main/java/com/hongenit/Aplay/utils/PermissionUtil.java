package com.hongenit.Aplay.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

/**
 * Created by hongenit on 2018/6/18.
 * desc:
 */

public class PermissionUtil {
    public static boolean requestPermission(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            ArrayList<String> unPermissioned = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    unPermissioned.add(permission);
                }
            }

            if (!unPermissioned.isEmpty()) {
                String[] unPermissionArray = new String[unPermissioned.size()];
                for (int i = 0; i < unPermissioned.size(); i++) {
                    unPermissionArray[i] = unPermissioned.get(i);
                }
                ActivityCompat.requestPermissions(activity, unPermissionArray, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean shouldShowExplain(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return false;
            }
        }
        return true;
    }


    public static void forwardSetting(Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, requestCode);
    }

}
