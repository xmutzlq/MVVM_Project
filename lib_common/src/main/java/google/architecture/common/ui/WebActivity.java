package google.architecture.common.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.king.android.res.config.ARouterPath;

import google.architecture.common.R;
import google.architecture.common.base.BaseActivity;
import google.architecture.common.databinding.ActivityWebPageBinding;
import google.architecture.common.util.CommKeyUtil;
import google.architecture.common.widget.CommWebView;

/**
 * @author lq.zeng
 * @date 2018/10/25
 */
@Route(path = ARouterPath.WebPage)
public class WebActivity extends BaseActivity<ActivityWebPageBinding> implements CommWebView.WebViewCallback {

    private CommWebView commWebView;

    @Override
    protected int getLayout() {
        return R.layout.activity_web_page;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCanBack(true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        commWebView = new CommWebView(this_);
        binding.commWebContainer.addView(commWebView, params);

        commWebView.setWebViewCallback(this);

        Intent intent = getIntent();
        String loadContent = intent.getStringExtra(CommKeyUtil.EXTRA_KEY);
        commWebView.loadUrl(loadContent);
    }

    @Override
    protected void onDestroy() {
        if(commWebView != null) commWebView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onReceivedTitle(String title) {
        setTitleName(title);
    }
}
