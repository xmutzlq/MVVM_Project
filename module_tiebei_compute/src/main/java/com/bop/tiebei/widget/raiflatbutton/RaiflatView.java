package com.bop.tiebei.widget.raiflatbutton;

import android.os.Parcelable;

/**
 * @author lq.zeng
 * @date 2018/8/8
 */

public interface RaiflatView {
    Parcelable onSaveInstanceState(Parcelable state);

    Parcelable onRestoreInstanceState(Parcelable state);

    void setFlatEnabled(boolean enable);

    boolean isFlatEnabled();

}
