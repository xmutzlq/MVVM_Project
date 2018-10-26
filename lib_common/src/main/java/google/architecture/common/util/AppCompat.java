package google.architecture.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewTreeObserver;

import google.architecture.common.selector.SelectorFactory;
import google.architecture.common.selector.SelectorShape;

/**
 * @author lq.zeng
 * @date 2018/5/21
 */

public final class AppCompat {

    public final static String WIDTH = "width";

    public final static String HEIGHT = "height";

    public static void setBackground(View view) {
        setBackground(view, null);
    }



    public static void setBackgroundPressed(Context context, View view, int colorRes) {
        setBackgroundPressed(context, view, colorRes, 0.8f);
    }

    public static void setBackgroundPressed(Context context, View view, int colorRes, float ratio) {
        int color = ContextCompat.getColor(context, colorRes);
        Drawable drawable = SelectorFactory.create(new SelectorShape.SelectorBuilder()
                .normalColor(color)
                .pressColor(AppCompat.darker(color, ratio)).build());
        setBackground(view, drawable);
    }

    public static void setBackground(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static void scheduleStartPostponedTransition(final Activity context, final View sharedElement) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //暂停转场动画
            sharedElement.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public boolean onPreDraw() {
                            sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                            context.startPostponedEnterTransition();
                            return true;
                        }
                    });
        }
    }

    // 明度
    public static int darker(int color, float ratio) {
        color = (color >> 24) == 0 ? 0x22808080 : color;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= ratio;
        return Color.HSVToColor(color >> 24, hsv);
    }
}
