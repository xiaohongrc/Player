package com.hongenit.Aplay.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by 陈小红 on 2016/11/20.
 * desc:
 */
public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("onCreateView----"+getClass().getSimpleName());
        return inflaterView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("onCreate----"+getClass().getSimpleName());
    }

    /**
     * 填充布局
     * @return
     */
    protected abstract View inflaterView();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        initData();
    }

    /**
     * 获取数据
     */
    protected void initData(){};
}
