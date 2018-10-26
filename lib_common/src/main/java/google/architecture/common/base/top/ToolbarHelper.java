package google.architecture.common.base.top;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import google.architecture.common.R;
import google.architecture.common.base.top.options.ToolbarOptions;

/**
 * @author lq.zeng
 * @date 2018/5/2
 */

public abstract class ToolbarHelper {
    public static final int TOOLBAR_TEMPLET_DEFUATL = R.layout.toolbar_templet_defuatl;

    public ToolbarHelper() {

    }

    public static ToolbarHelper Create(@NonNull ToolbarView uiView, View rootView) {
        int toolbarLayout = uiView.getToolbarLayout();
        if (toolbarLayout == TOOLBAR_TEMPLET_DEFUATL) {
            return new ToolbarHelperImpl(uiView, rootView, toolbarLayout);
        } else {
            return new EmptyToolbarHelperImpl(uiView, rootView, toolbarLayout);
        }
    }

    /**
     * 快速设置Toolbar,取消边距,隐藏所有默认的显示.
     * setDisplayShowCustomEnabled(),setDisplayHomeAsUpEnabled()
     * setDisplayShowTitleEnabled(),setDisplayShowHomeEnable()
     * 都将设置为false
     *
     * @param context 继承Appcompat的activity的上下文
     * @param toolbar 将要设置的Toolbar
     */

    public static void SimpleInitToolbar(Context context, @NonNull Toolbar toolbar, boolean isMaterialDesign) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            toolbar.setContentInsetsAbsolute(0, 0);
            activity.setSupportActionBar(toolbar);
            ActionBar supportActionBar = activity.getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.setDisplayShowCustomEnabled(isMaterialDesign);
                supportActionBar.setDisplayHomeAsUpEnabled(isMaterialDesign);
                supportActionBar.setDisplayShowTitleEnabled(isMaterialDesign);
                supportActionBar.setDisplayShowHomeEnabled(isMaterialDesign);
            }
        }
    }

    /**
     * 设置滑动Flag
     *
     * @param viewId
     * @param flag
     * @return
     */
    public abstract boolean setScrollFlag(@IdRes int viewId, @AppBarLayout.LayoutParams.ScrollFlags int flag);

    /**
     * 获取AppBarLayout中的View
     *
     * @param viewId
     * @param <V>
     * @return
     */
    public abstract <V extends View> V findViewFromAppBar(@IdRes int viewId);

    /**
     * 获取AppBarLayout
     *
     * @return
     */
    public abstract AppBarLayout getAppBarLayout();

    /**
     * 获取Toolbar
     *
     * @return
     */
    public abstract Toolbar getToolbar();

    /**
     * 获取Toolbar配置
     *
     * @return
     */
    public abstract void setToolbarOptions(ToolbarOptions toolbarOptions);

    /**
     * 设置title
     *
     * @param str
     */

    public abstract void setTitle(@NonNull String str);

    public abstract void setTitle(@StringRes int str);

    /**
     * 设置左边
     *
     * @param strId
     */
    public abstract void setLeftText(@StringRes int strId, View.OnClickListener clickListener);

    public abstract void setLeftText(@NonNull String str, View.OnClickListener clickListener);

    public abstract void setLeftButton(Drawable drawable, View.OnClickListener clickListener);

    public abstract void setLeftButton(@DrawableRes int drawableId, View.OnClickListener clickListener);


    /**
     * 设置右边
     *
     * @param str
     */
    public abstract void setRightText(@NonNull String str, View.OnClickListener clickListener);

    public abstract void setRightText(@StringRes int strId, View.OnClickListener clickListener);

    public abstract void setRightButton(@NonNull Drawable drawable, View.OnClickListener clickListener);

    public abstract void setRightButton(@DrawableRes int drawableId, View.OnClickListener clickListener);


    public abstract void setCanBack(boolean canback);
}

