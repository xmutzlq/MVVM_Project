package com.bop.tiebei.widget;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bop.tiebei.R;

import google.architecture.common.util.AppCompat;
import google.architecture.common.util.DimensionsUtil;
import google.architecture.common.util.KeyboardUtils;
import google.architecture.coremodel.util.TextUtil;

/**
 * @author lq.zeng
 * @date 2018/8/3
 */

public class RectEditView extends SearchView implements KeyboardUtils.SoftKeyboardToggleListener {

    private boolean isKeyBoardShown;
    private boolean isTextViewState;
    private SearchView.SearchAutoComplete mEdit;

    public RectEditView(Context context) {
        super(context);
        init();
    }

    public RectEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setText(CharSequence inputValue) {
        setText(inputValue, true);
    }

    public void setText(CharSequence inputValue, boolean isNeedSelection) {
        mEdit.setText(inputValue);
        if(!TextUtils.isEmpty(inputValue) && isNeedSelection) mEdit.setSelection(inputValue.length());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyboardUtils.addKeyboardToggleListener((Activity) getContext(), this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyboardUtils.removeKeyboardToggleListener(this);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && !isKeyBoardShown) {
            return false;
        } else {
            KeyboardUtils.closeSoftInput(getContext(), this);
            return true;
        }
    }

    private void init() {
        mEdit = findViewById(R.id.search_src_text);
        mEdit.setPadding(DimensionsUtil.dip2px(getContext(), 2),0,DimensionsUtil.dip2px(getContext(), 2),0);
        deleteUnderline();
        changeSearchIcon();
        changeSearchText();
        changeSearchDelIcon();

        setIconifiedByDefault(false);
        setIconified(true);
        onActionViewExpanded();

        setFocusable(false);
        setFocusableInTouchMode(true);
        clearFocus();

    }

    /**改变searchview清理图标*/
    private void changeSearchDelIcon() {
        changeSearchDelIcon(R.mipmap.ic_text_clear);
    }

    public void changeSearchDelIcon(int clearRes) {
        ImageView closeViewIcon = findViewById(R.id.search_close_btn);
        if(clearRes > 0) {
            closeViewIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), clearRes));
        } else {
            closeViewIcon.setPadding(0,0,0,0);
            closeViewIcon.setImageDrawable(null);
            closeViewIcon.setEnabled(false);
        }
    }

    /**改变searchview文本*/
    private void changeSearchText() {
        final EditText editText = findViewById(R.id.search_src_text);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimension(R.dimen.text_size_14sp));
        editText.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_88));
        editText.setHintTextColor(ContextCompat.getColor(getContext(), R.color.gray_D5));
        editText.setOnFocusChangeListener((v, hasFocus) -> {});
    }

    public void enable(boolean enable) {
        final EditText editText = findViewById(R.id.search_src_text);
        editText.setEnabled(enable);
    }

    public void setFilters(InputFilter[] filters) {
        final EditText editText = findViewById(R.id.search_src_text);
        if(filters != null) editText.setFilters(filters);
    }

    public void addTextWatch(TextWatcher textWatcher) {
        final EditText editText = findViewById(R.id.search_src_text);
        if(textWatcher != null) editText.addTextChangedListener(textWatcher);
    }

    public static class EmptyToZeroTextWatch implements TextWatcher {

        private RectEditView mEditText;

        public EmptyToZeroTextWatch(RectEditView editText) {
            mEditText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(TextUtil.isEmpty(s.toString())) {
                mEditText.setText("0", false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public boolean isTextViewState() {
        return isTextViewState;
    }

    /**丢弃EditText功能*/
    public void switchToTextView() {
        isTextViewState = true;
        changeSearchDelIcon(0);
        final EditText editText = findViewById(R.id.search_src_text);
        editText.setEnabled(false);
    }

    /**改变searchview搜索图标*/
    private void changeSearchIcon() {
        ImageView searchViewIcon = findViewById(R.id.search_mag_icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        searchViewIcon.setLayoutParams(lp);
        searchViewIcon.setImageResource(0);
    }

    /**去掉searchview下划线*/
    private void deleteUnderline() {
        AppCompat.setBackground(findViewById(android.support.v7.appcompat.R.id.search_plate));
        AppCompat.setBackground(findViewById(android.support.v7.appcompat.R.id.submit_area));
    }

    @Override
    public void onToggleSoftKeyboard(boolean isVisible, int heightDifference) {
        isKeyBoardShown = isVisible;
    }
}
