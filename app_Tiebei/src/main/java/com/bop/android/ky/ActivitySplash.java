package com.bop.android.ky;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.alibaba.android.arouter.launcher.ARouter;
import com.bop.android.ky.databinding.ActivitySplashBinding;
import com.king.android.res.config.ARouterPath;

import java.util.concurrent.TimeUnit;

import google.architecture.common.base.BaseActivity;
import google.architecture.common.base.ViewManager;
import google.architecture.common.viewmodel.UpdateViewModel;
import google.architecture.coremodel.data.VersionInfo;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author lq.zeng
 * @date 2018/4/25
 */

public class ActivitySplash extends BaseActivity<ActivitySplashBinding>{

    private UpdateViewModel updateViewModel;
    private CompositeDisposable disposable;

    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_splash;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setBackgroundDrawable(null);
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                ViewManager.getInstance().finishActivity();
                return;
            }
        }

        updateViewModel = new UpdateViewModel();
        addRunStatusChangeCallBack(updateViewModel);
        updateViewModel.getVersionInfo();
    }

    @Override
    protected void onDataResult(Object o) {
        VersionInfo info = (VersionInfo)o;
        Bundle bundle = new Bundle();
        bundle.putParcelable(App.EXTRA_NAME_VERSION_INFO, info);

        disposable = new CompositeDisposable();
        disposable.add(Observable.timer(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if(disposable != null) disposable.clear();
                    ARouter.getInstance()
                            .build(ARouterPath.AppMainTieBeiAty)
                            .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .withTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            .with(bundle)
                            .navigation(this_);
                    ViewManager.getInstance().finishActivity();
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(disposable != null) disposable.clear();
    }
}
