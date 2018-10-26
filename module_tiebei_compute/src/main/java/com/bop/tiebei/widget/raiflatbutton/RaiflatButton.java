package com.bop.tiebei.widget.raiflatbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcelable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import com.bop.tiebei.R;

/**
 * @author lq.zeng
 * @date 2018/8/8
 */

public class RaiflatButton extends AppCompatButton {

    private RaiflatDelegate mDelegate;

    public RaiflatButton(Context context) {
        this(context, null);
    }

    public RaiflatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RaiflatButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDelegate = new RaiflatDelegate(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs,
                    R.styleable.RaiflatButton);
            mDelegate.setup(typedArray.getBoolean(R.styleable.RaiflatButton_flatten, true));
            typedArray.recycle();
        }
    }

    public void setFlatEnabled(boolean flatten) {
        mDelegate.setFlatEnabled(flatten);
    }

    public boolean isFlatEnabled() {
        return mDelegate.isFlatEnabled();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return mDelegate.onSaveInstanceState(super.onSaveInstanceState());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(mDelegate.onRestoreInstanceState(state));
    }
}
