package com.bop.android.ky;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.king.android.res.config.ARouterPath;
import com.kongzue.dialog.v2.SelectDialog;

import google.architecture.common.base.BaseActivity;
import google.architecture.common.base.BaseFragment;
import google.architecture.common.base.ViewManager;
import google.architecture.common.upgrade.UpdateManager;
import google.architecture.common.util.AppUtil;
import google.architecture.coremodel.data.VersionInfo;
import io.reactivex.Observable;

/**
 * @author lq.zeng
 * @date 2018/8/13
 */
@Route(path = ARouterPath.AppMainTieBeiAty)
public class ActivityMainTieBei extends BaseActivity{

    private VersionInfo versionInfo;

    @Override
    protected int getLayout() {
        return R.layout.activity_main_tiebei;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitleName(R.string.title_compute);
        setCanBack(false);

        Intent intent = getIntent();
        if(intent != null) {
            Bundle bundle = intent.getExtras();
            if(bundle != null) versionInfo = bundle.getParcelable(App.EXTRA_NAME_VERSION_INFO);
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Looper.myQueue().addIdleHandler(() -> {

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container_compute, (BaseFragment) ARouter.getInstance().build(ARouterPath.T_ComputeFgt).navigation())
                    .commit();
            return false;
        });

        //升级
        Observable.just(versionInfo).filter(versionInfo1 -> versionInfo1 != null).subscribe(versionInfo1 -> {
            int version = AppUtil.getVersionCode(this_); // 本地版本号
            if (!TextUtils.isEmpty(versionInfo1.getVersion_code())
                    && Integer.valueOf(versionInfo1.getVersion_code()) > version) { // 允许更新
                new UpdateManager(this_, versionInfo1).update();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SelectDialog.show(this_, getResources().getString(R.string.str_warmming_tip),
                    getResources().getString(R.string.str_exit_tip), getResources().getString(R.string.btn_comfirm), (dialog, which) -> {
                        ViewManager.getInstance().exitApp(this_);
                    }, getResources().getString(R.string.btn_cancel), (dialog, which) -> {});
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
