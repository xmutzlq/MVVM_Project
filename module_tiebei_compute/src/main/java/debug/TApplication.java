package debug;

import com.alibaba.android.arouter.launcher.ARouter;

import google.architecture.common.base.BaseApplication;
import google.architecture.common.util.Utils;

/**
 * @author lq.zeng
 * @date 2018/8/3
 */

public class TApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        if (Utils.isAppDebug()) {
            //开启InstantRun之后，一定要在ARouter.init之前调用openDebug
            ARouter.openDebug();
            ARouter.openLog();
        }
        ARouter.init(this);
    }
}
