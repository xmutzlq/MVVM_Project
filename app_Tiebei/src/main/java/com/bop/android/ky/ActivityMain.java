package com.bop.android.ky;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.apkfuns.logutils.LogUtils;
import com.bop.android.ky.databinding.ActivityMainBinding;
import com.king.android.res.config.ARouterPath;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import google.architecture.common.base.BaseActivity;
import google.architecture.common.base.BaseApplication;
import google.architecture.common.base.BaseFragment;
import google.architecture.common.widget.NoScrollViewPager;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;

@Route(path = ARouterPath.AppMainAty)
public class ActivityMain extends BaseActivity<ActivityMainBinding> implements BottomNavigationView.OnNavigationItemSelectedListener{

    private NoScrollViewPager mPager;
    private List<BaseFragment> mFragments = new ArrayList<>();
    private FragmentAdapter mAdapter;

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPager = binding.containerPager;
        mPager.setOffscreenPageLimit(3);
        mPager.setScanScroll(false);

        mAdapter = new FragmentAdapter(getSupportFragmentManager(), mFragments);
        binding.setViewPaAdapter(mAdapter);

        requestPermissions();
    }

    private void requestPermissions() {
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission.requestEach(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS)
                .subscribe(permission->{
                    if (permission.granted) {
                        // 用户已经同意该权限
                        LogUtils.tag("zlq").e(permission.name + " is granted.");
                    } else if (permission.shouldShowRequestPermissionRationale) {
                        // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                        LogUtils.tag("zlq").e(permission.name + " is denied. More info should be provided.");
                    } else {
                        // 用户拒绝了该权限，并且选中『不再询问』
                        LogUtils.tag("zlq").e( permission.name + " is denied.");
                    }
                });
    }

    @Override
    public void onUserLoginStateChange(boolean isLogin) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}
