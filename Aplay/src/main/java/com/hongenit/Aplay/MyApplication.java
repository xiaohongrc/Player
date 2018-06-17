package com.hongenit.Aplay;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;


/**
 * Created by hongenit on 2016/12/11.
 * desc:
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initUmeng();
        initAdMob();
    }

    private void initUmeng() {
        UMConfigure.init(this, Constants.UMENG_APP_ID, Constants.UMENG_CHANNEL_NAME, UMConfigure.DEVICE_TYPE_PHONE, null);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
        UMConfigure.setLogEnabled(true);
    }

    private void initAdMob() {
        MobileAds.initialize(this, Constants.ADMOB_APP_ID);
    }


}
