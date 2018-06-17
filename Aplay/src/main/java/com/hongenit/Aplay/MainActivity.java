package com.hongenit.Aplay;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.hongenit.Aplay.base.BaseFragmentActivity;
import com.hongenit.Aplay.fragment.LocalFragment;
import com.hongenit.Aplay.utils.LogUtil;
import com.hongenit.Aplay.utils.PermissionUtil;

public class MainActivity extends BaseFragmentActivity {
    private static final int REQUEST_CODE_STORAGE = 1001;
    private long mLastExitTime;
    private AdView mAdView;
    private ViewGroup mFragmentContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        request_permission();
    }

    private void request_permission() {
        if (PermissionUtil.requestPermission(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE)) {
            loadLocalFragment();
        }
    }

    private void initView() {
        mAdView = findViewById(R.id.adView);
        mFragmentContainer = findViewById(R.id.fragment_local_video);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAd();
    }

    private void loadLocalFragment() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_local_video, LocalFragment.newInstance());
        transaction.commit();
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                LogUtil.i(this, "onAdLoaded()");
            }
        });

    }


    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - mLastExitTime < 2000) {
            super.onBackPressed();
        } else {
            mLastExitTime = System.currentTimeMillis();
            Toast.makeText(this, R.string.tip_click_again_exist, Toast.LENGTH_LONG)
                    .show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = false;
        for (int result : grantResults) {
            LogUtil.d(this, "result = " + result);
            if (result == PackageManager.PERMISSION_GRANTED) {
                isGranted = true;
            }
        }
        LogUtil.d(this, "requestCode = " + requestCode);
        if (!isGranted) {
            boolean showExplain = PermissionUtil.shouldShowExplain(this, permissions);
            LogUtil.d(this, "showExplain = " + showExplain);
            if (showExplain) {
                Toast.makeText(this, R.string.request_permission_storage, Toast.LENGTH_LONG).show();
                request_permission();
            } else {
                Toast.makeText(this, R.string.request_permission_storage, Toast.LENGTH_LONG).show();
                PermissionUtil.forwardSetting(this, REQUEST_CODE_STORAGE);
            }
        } else {
            loadLocalFragment();
        }

    }


}
