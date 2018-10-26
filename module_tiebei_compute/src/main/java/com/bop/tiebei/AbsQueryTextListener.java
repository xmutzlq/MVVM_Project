package com.bop.tiebei;

import android.support.v7.widget.SearchView;

import google.architecture.coremodel.util.TextUtil;

/**
 * @author lq.zeng
 * @date 2018/8/7
 */

public abstract class AbsQueryTextListener implements SearchView.OnQueryTextListener {

    public boolean isNeedQueryTextChange = true;

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(isNeedQueryTextChange) {
            if(TextUtil.isEmpty(newText)) {
                newText = "0";
            }
            onTextChange(Double.valueOf(newText));
        }
        return true;
    }

    public abstract void onTextChange(double value);
}
