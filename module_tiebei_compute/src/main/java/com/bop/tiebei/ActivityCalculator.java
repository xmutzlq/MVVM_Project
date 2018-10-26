package com.bop.tiebei;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import google.architecture.common.base.BaseActivity;

/**
 * @author lq.zeng
 * @date 2018/8/8
 */

public class ActivityCalculator extends BaseActivity {

    public static void openActivityCalculator(Context context, String str) {
        Intent intent = new Intent(context, ActivityCalculator.class);
        intent.putExtra("extra_name", str);
        context.startActivity(intent);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_calculator;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String extraValue = getIntent().getStringExtra("extra_name");
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container_calculator, FragmentCalculator.newInstance(extraValue),
                        FragmentCalculator.class.getSimpleName()).commit();
    }
}
