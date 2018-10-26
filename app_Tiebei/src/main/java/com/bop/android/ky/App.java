package com.bop.android.ky;

import com.alibaba.android.arouter.launcher.ARouter;
import com.facebook.drawee.backends.pipeline.Fresco;

import google.architecture.common.base.BaseApplication;
import google.architecture.common.util.Utils;

public class App extends BaseApplication{

    public static final String EXTRA_NAME_VERSION_INFO = "version_info";

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        if (Utils.isAppDebug()) {
            //开启InstantRun之后，一定要在ARouter.init之前调用openDebug
            ARouter.openDebug();
            ARouter.openLog();
        }
        ARouter.init(this);
    }
}
