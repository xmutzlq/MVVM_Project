package com.bop.tiebei;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.king.android.res.config.ARouterPath;
import com.kongzue.dialog.v2.SelectDialog;

import google.architecture.common.base.BaseActivity;
import google.architecture.common.base.ViewManager;

/**
 * @author lq.zeng
 * @date 2018/8/3
 */

@Route(path = ARouterPath.T_ComputeAty)
public class ActivityCompute extends BaseActivity{

    @Override
    protected int getLayout() {
        return R.layout.activity_compute;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleName(R.string.t_title_compute);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container_compute, FragmentCompute.newInstance(),
                        FragmentCompute.class.getSimpleName())
                .commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SelectDialog.show(this_, getResources().getString(R.string.t_str_warmming_tip),
                    getResources().getString(R.string.t_str_exit_tip), getResources().getString(R.string.t_btn_comfirm), (dialog, which) -> {
                        ViewManager.getInstance().exitApp(this_);
                    }, getResources().getString(R.string.t_btn_cancel), (dialog, which) -> {});
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
