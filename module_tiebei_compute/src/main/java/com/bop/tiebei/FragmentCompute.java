package com.bop.tiebei;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.view.TimePickerView;
import com.bop.tiebei.util.PriceUtil;
import com.bop.tiebei.widget.RectEditView;
import com.king.android.res.config.ARouterPath;
import com.kongzue.dialog.v2.MessageDialog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import google.architecture.common.base.BaseFragment;
import google.architecture.common.util.TimeUtils;
import google.architecture.common.util.ToastUtils;
import google.architecture.common.widget.CustomPopWindow;
import google.architecture.coremodel.util.TextUtil;
import io.reactivex.Observable;

/**
 * @author lq.zeng
 * @date 2018/8/3
 */
@Route(path = ARouterPath.T_ComputeFgt)
public class FragmentCompute extends BaseFragment {

    private static final String TIME_FORMAT = "yyyy-MM-dd";
    private static final String TYPE_ONE_YEAR_ELC = "one_year_elc";
    private static final String TYPE_HALF_YEAR_ELC = "half_year_elc";
    private static final String TYPE_HALF_YEAR_PAPER = "half_year_paper";

    private List<RectEditView> mRectEditViews;
    private List<TextView> mWeekViews;
    private List<TextView> mResultViews;
    private List<SearchView.OnQueryTextListener> mRectEditListener;

    private ClipboardManager cm;

    private String currentType = TYPE_ONE_YEAR_ELC;
    private String tRate;
    private double toCalculator1, toCalculator2, toCalculator3;

    public static FragmentCompute newInstance() {
        return new FragmentCompute();
    }

    @Override
    protected int getLayout() {
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compute, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        mResultViews = new ArrayList<>();
        mWeekViews = new ArrayList<>();
        mRectEditViews = new ArrayList<>();
        mRectEditListener = new ArrayList<>();

        //Top
        String[] leftLabels = getResources().getStringArray(R.array.compute_left_label);
        String[] rightLabels = getResources().getStringArray(R.array.compute_right_label);
        final ViewGroup viewTops = (ViewGroup) findViewById(view, R.id.layout_compute_top);

        //Label
        Observable.range(0, viewTops.getChildCount()).flatMap(integer -> {
            View rootView = viewTops.getChildAt(integer);
            rootView.setTag(leftLabels[integer]);
            return Observable.just(rootView);
        }).subscribe(view1 -> {
            String label = (String)view1.getTag();
            ((TextView)view1.findViewById(R.id.tv_compute_label)).setText(label);
        });

        //当前日期
        String curTime = TimeUtils.getNowTimeString(TIME_FORMAT);
        String curWeek = TimeUtils.getWeek(TimeUtils.getNowTimeMills());

        //一年后
        String afterAYearDate[] = TComputeUtil.timeCompute(Calendar.YEAR, TimeUtils.getNowTimeString(), 1);
        String afterAYearTime = afterAYearDate[0];
        String afterAYearWeek = afterAYearDate[1];

        //输入框
        Observable.range(0, viewTops.getChildCount()).flatMap(integer -> {
            View rootView = viewTops.getChildAt(integer);
            rootView.setTag(new MyViewTag(rightLabels[integer]));
            return Observable.just(rootView);
        }).map(view1 -> {
            if(view1.getTag() != null && view1.getTag() instanceof MyViewTag) {
                MyViewTag viewTag = (MyViewTag)view1.getTag();
                if(viewTag.tag.equals("0000-00-00")) {
                    view1.setTag(new MyViewTag(viewTag.tag, MyViewTag.TYPE_TEXT, curTime + " " + curWeek, true));
                } else if(viewTag.tag.equals("0001-00-00")) {
                    view1.setTag(new MyViewTag(viewTag.tag, MyViewTag.TYPE_TEXT, afterAYearTime + " " + afterAYearWeek, true));
                } else if(viewTag.tag.equals("0_c")) {
                    view1.setTag(new MyViewTag(viewTag.tag, MyViewTag.TYPE_CHOICE, "0"));
                }
            }
            return view1;
        }).subscribe(view1 -> {
            if(view1.getTag() != null && view1.getTag() instanceof MyViewTag) {
                MyViewTag viewTag = (MyViewTag)view1.getTag();
                final RectEditView rectEditView = view1.findViewById(R.id.et_compute_input);
                mRectEditViews.add(rectEditView);
                switch (viewTag.type) {
                    case MyViewTag.TYPE_EDIT:
                        rectEditView.setQueryHint(viewTag.value);
                        //只能数字和小数点
                        rectEditView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        break;
                    case MyViewTag.TYPE_TEXT:
                        rectEditView.switchToTextView();
                        String[] dateStr = viewTag.value.split(" ");
                        rectEditView.setText(dateStr[0]);
                        TextView weekView = view1.findViewById(R.id.tv_compute_date);
                        mWeekViews.add(weekView);
                        weekView.setText(dateStr[1]);
                        view1.findViewById(R.id.btn_compute_date).setTag(viewTag.tag);
                        view1.findViewById(R.id.btn_compute_date).setOnClickListener(v -> {
                            //时间选择器
                            TimePickerView pvTime = new TimePickerBuilder(mContext, (date, v1) -> {
                                if(rectEditView != null) rectEditView.setText(TimeUtils.date2String(date, "yyyy-MM-dd"));
                                if(weekView != null) weekView.setText(TimeUtils.getWeek(date));
                                if(v.getTag().toString().equals("0001-00-00")) {
                                    boolean isPaper = currentType.equals(TYPE_HALF_YEAR_PAPER);
                                    mRectEditViews.get(6).setText(TComputeUtil.getAdjustmentDayNum(isPaper, rectEditView.getQuery().toString()));
                                }
                            }).build();
                            Calendar cal = new GregorianCalendar();
                            cal.setTime(TimeUtils.string2Date(rectEditView.getQuery().toString(), "yyyy-MM-dd"));
                            pvTime.setDate(cal);
                            pvTime.show();
                        });
                        break;
                    case MyViewTag.TYPE_CHOICE:
                        rectEditView.changeSearchDelIcon(0);
                        rectEditView.setQueryHint(viewTag.value);
                        rectEditView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        rectEditView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
//                        rectEditView.addTextWatch(new RectEditView.EmptyToZeroTextWatch(rectEditView));
                        ViewGroup choiceViewGroup = view1.findViewById(R.id.layout_compute_choice_root);
                        //一年电
                        choiceViewGroup.findViewById(R.id.tv_compute_one_year).setOnClickListener(v -> {
                            currentType = TYPE_ONE_YEAR_ELC;
                            choice(choiceViewGroup, 1);
                            String[] value = TComputeUtil.getAdjustmentDate(0, mRectEditViews.get(4).getQuery().toString());
                            updateEndDate(value);
                            if(rectEditView != null) rectEditView.setText(TComputeUtil.getAdjustmentDayNum(false, mRectEditViews.get(5).getQuery().toString()));
                        });
                        //半年电
                        choiceViewGroup.findViewById(R.id.tv_compute_half_year).setOnClickListener(v -> {
                            currentType = TYPE_HALF_YEAR_ELC;
                            choice(choiceViewGroup, 2);
                            String[] value = TComputeUtil.getAdjustmentDate(1, mRectEditViews.get(4).getQuery().toString());
                            updateEndDate(value);
                            if(rectEditView != null) rectEditView.setText(TComputeUtil.getAdjustmentDayNum(false, mRectEditViews.get(5).getQuery().toString()));
                        });
                        //半年纸
                        choiceViewGroup.findViewById(R.id.tv_compute_half_paper).setOnClickListener(v -> {
                            currentType = TYPE_HALF_YEAR_PAPER;
                            choice(choiceViewGroup, 3);
                            String[] value = TComputeUtil.getAdjustmentDate(2, mRectEditViews.get(4).getQuery().toString());
                            updateEndDate(value);
                            if(rectEditView != null) rectEditView.setText(TComputeUtil.getAdjustmentDayNum(true, mRectEditViews.get(5).getQuery().toString()));
                        });

                        currentType = TYPE_ONE_YEAR_ELC;
                        rectEditView.setText(TComputeUtil.getAdjustmentDayNum(false, mRectEditViews.get(5).getQuery().toString()));
                        choice(choiceViewGroup, 1);
                        break;
                }
            }
        });

        onTextChange();

        //Bottom
        String[] leftBottomLabels = getResources().getStringArray(R.array.compute_left_bottom_lable);
        String[] rightBottomLabels = getResources().getStringArray(R.array.compute_right_bottom_label);
        final ViewGroup viewBottoms = (ViewGroup) findViewById(view, R.id.layout_compute_bottom);

        //Label
        Observable.range(0, viewBottoms.getChildCount()).flatMap(integer -> {
            View rootView = viewBottoms.getChildAt(integer);
            rootView.setTag(leftBottomLabels[integer]);
            return Observable.just(rootView);
        }).subscribe(view1 -> {
            String label = (String)view1.getTag();
            ((TextView)view1.findViewById(R.id.tv_compute_label)).setText(label);
        });

        //result
        Observable.range(0, viewBottoms.getChildCount()).flatMap(integer -> {
            View rootView = viewBottoms.getChildAt(integer);
            rootView.setTag(rightBottomLabels[integer]);
            return Observable.just(rootView);
        }).subscribe(view1 -> {
            String label = (String)view1.getTag();
            TextView resultView = view1.findViewById(R.id.et_compute_result);
            resultView.setMaxLines(1); //1行显示
            mResultViews.add(resultView);
            resultView.setText(label);
            resultView.setOnClickListener(v -> {
                ClipData clipData = ClipData.newPlainText(null, resultView.getText().toString());
                cm.setPrimaryClip(clipData);
                ToastUtils.showShortToast("已复制到粘贴板");
            });
            resultView.setOnLongClickListener(v -> {
                TextView textView = new TextView(mContext);
                textView.setMaxEms(3);
                textView.setMaxLines(2);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                textView.setTextColor(ContextCompat.getColor(mContext, R.color.white));
                textView.setText(resultView.getText().toString());
                textView.setBackgroundResource(R.mipmap.pop_bg2);
                textView.setFocusable(true);
                textView.setFocusableInTouchMode(true);
                CustomPopWindow popWindow = new CustomPopWindow.PopupWindowBuilder(mContext)
                        .setView(textView)
                        .enableBackgroundDark(false) //弹出popWindow时，背景是否变暗
                        .setBgDarkAlpha(0.7f) // 控制亮度
                        .create();
                popWindow.showAsDropDown(resultView,0, - (resultView.getHeight() + popWindow.getHeight()));
                return false;
            });
            View calculatorView1 = view1.findViewById(R.id.btn_calculator_1);
            View calculatorView2 = view1.findViewById(R.id.btn_calculator_2);
            View calculatorView3 = view1.findViewById(R.id.btn_calculator_3);
            if(calculatorView1 != null) {
                calculatorView1.setOnClickListener(v -> {
                    DecimalFormat df = new DecimalFormat("0.00");
                    ActivityCalculator.openActivityCalculator(mContext, df.format(toCalculator1));
                });
            }

            if(calculatorView2 != null) {
                calculatorView2.setOnClickListener(v -> {
                    DecimalFormat df = new DecimalFormat("0.00");
                    ActivityCalculator.openActivityCalculator(mContext, df.format(toCalculator2));
                });
            }

            if(calculatorView3 != null) {
                calculatorView3.setOnClickListener(v -> {
                    DecimalFormat df = new DecimalFormat("0.00");
                    ActivityCalculator.openActivityCalculator(mContext, df.format(toCalculator3));
                });
            }
        });

        //清空
        ((View)findViewById(view, R.id.btn_clear)).setOnClickListener(v -> {
            Observable.fromIterable(mRectEditViews).subscribe(editView -> {
                if(!editView.isTextViewState()) {
                    editView.setText("");
                }
            });
            Observable.range(0, mResultViews.size()).flatMap(integer -> {
                TextView textView = mResultViews.get(integer);
                textView.setTag(rightBottomLabels[integer]);
                return Observable.just(textView);
            }).subscribe(textView -> {
                textView.setText((String)textView.getTag());
            });
        });

        //计算
        ((View)findViewById(view, R.id.btn_compute)).setOnClickListener(v -> {
            String money = mRectEditViews.get(0).getQuery().toString(); //票面金额
            String yearRate = mRectEditViews.get(2).getQuery().toString(); //年化率
            String deductMoney = mRectEditViews.get(3).getQuery().toString(); //手续费
            String curDate = mRectEditViews.get(4).getQuery().toString(); //贴现日期
            String endDate = mRectEditViews.get(5).getQuery().toString(); //到期日期
            String adjustmentDate = mRectEditViews.get(6).getQuery().toString(); //调整日期
            if(TextUtil.isEmpty(adjustmentDate)) adjustmentDate = "0";
            long date = TComputeUtil.getDates(endDate, curDate, Integer.valueOf(adjustmentDate));
            if(TextUtil.isEmpty(money)) money = "0";
            if(TextUtil.isEmpty(deductMoney)) deductMoney = "0";
            if(TextUtil.isEmpty(yearRate)) yearRate = "0";
            double yuanMoney = Double.valueOf(money) * 10000;
            double tMoney = TComputeUtil.getDiscountAmount(yuanMoney, Double.valueOf(yearRate) / 100, date, Double.valueOf(deductMoney));
            tRate = String.valueOf(yuanMoney - Double.valueOf(tMoney));
            mResultViews.get(0).setText(String.valueOf(date)); //计息天数
            toCalculator1 = yuanMoney == 0 ? 0 : Double.valueOf(tRate) / yuanMoney * 100000;
            toCalculator2 = yuanMoney - tMoney;
            toCalculator3 = tMoney;
            mResultViews.get(1).setText(PriceUtil.getFormatPrice(mContext, toCalculator1)); //十万贴息
            mResultViews.get(2).setText(PriceUtil.getFormatPrice(mContext, toCalculator2)); //贴现利息
            mResultViews.get(3).setText(PriceUtil.getFormatPrice(mContext, toCalculator3)); //贴现金额
        });

        //计算汇率
        ((View)findViewById(view, R.id.et_compute_rate)).setOnClickListener(v -> {
            DecimalFormat df = new DecimalFormat("0.000");
            String money = mRectEditViews.get(0).getQuery().toString(); //票面金额

            String curDate = mRectEditViews.get(4).getQuery().toString(); //贴现日期
            String endDate = mRectEditViews.get(5).getQuery().toString(); //到期日期
            String adjustmentDate = mRectEditViews.get(6).getQuery().toString(); //调整日期

            long date = TComputeUtil.getDates(endDate, curDate, Integer.valueOf(adjustmentDate));

            if(TextUtil.isEmpty(money)) money = "0";
            if(TextUtil.isEmpty(tRate)) tRate = "0";

            double yuanMoney = Double.valueOf(money) * 10000;
            double yearRated = Double.valueOf(tRate) / date / yuanMoney * 360 * 100;
            String yearRate = df.format(yearRated); //百分位
            String monthRate = df.format(yearRated / 12 * 10); //千分位
            if(monthRate.equalsIgnoreCase("NaN")) monthRate = "0.000";
            if(yearRate.equalsIgnoreCase("NaN")) yearRate = "0.000";
            String message = mContext.getString(R.string.t_str_rate_month) + monthRate + "\n"
                    + mContext.getString(R.string.t_str_rate_year) + yearRate;
            MessageDialog.show(mContext, mContext.getResources().getString(R.string.t_str_rate_compute),
                    message, mContext.getResources().getString(R.string.t_btn_comfirm), (dialog, which) -> {
            });
        });
    }

    /**
     * 到期日
     * @param date 时间和星期
     */
    private void updateEndDate(String date[]) {
        if(date.length < 2) return;
        mRectEditViews.get(5).setText(date[0]);
        mWeekViews.get(1).setText(date[1]);
    }

    /**
     * 调整天数选择
     * @param rootView
     * @param position
     */
    private void choice(ViewGroup rootView, int position) {
        if(rootView == null) return;
        Observable.range(0, rootView.getChildCount()).flatMap(integer -> {
            View choiceView = rootView.getChildAt(integer);
            choiceView.setTag(String.valueOf(integer));
            choiceView.setPressed(false);
            choiceView.setSelected(false);
            return Observable.just(choiceView);
        }).filter(view -> String.valueOf(view.getTag()).equals(String.valueOf(position))).subscribe(view -> {
            view.setPressed(true);
            view.setSelected(true);
        });
    }

    /**顺序根据具体界面定制监听**/
    private void onRegisterListener() {
        mRectEditListener.add(new MoneyTextListener());
        mRectEditListener.add(new MonthextListener());
        mRectEditListener.add(new YearTextListener());
        mRectEditListener.add(new TenTextListener());
        mRectEditListener.add(new CurDateTextListener());
        mRectEditListener.add(new EndDateTextListener());
        mRectEditListener.add(new DayTextListener());
    }

    private void onTextChange() {
        onRegisterListener();
        if(mRectEditListener == null || mRectEditListener.size() == 0) return;
        Observable.range(0, mRectEditListener.size()).subscribe(integer -> {
            mRectEditViews.get(integer).setOnQueryTextListener(mRectEditListener.get(integer));
        });
    }

    private class MoneyTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    private class MonthextListener extends AbsQueryTextListener {
        @Override
        public void onTextChange(double value) {
            isNeedQueryTextChange = true;
            ((AbsQueryTextListener)mRectEditListener.get(2)).isNeedQueryTextChange = false;
            double yearRate = value * 12 / 10;
            DecimalFormat df = new DecimalFormat("0.0000");
            mRectEditViews.get(2).setText(df.format(yearRate));
            ((AbsQueryTextListener)mRectEditListener.get(2)).isNeedQueryTextChange = true;
        }
    }

    private class YearTextListener extends AbsQueryTextListener {
        @Override
        public void onTextChange(double value) {
            isNeedQueryTextChange = true;
            ((AbsQueryTextListener)mRectEditListener.get(1)).isNeedQueryTextChange = false;
            double monthRate = value / 12  * 10;
            DecimalFormat df = new DecimalFormat("0.0000");
            mRectEditViews.get(1).setText(df.format(monthRate));
            ((AbsQueryTextListener)mRectEditListener.get(1)).isNeedQueryTextChange = true;
        }
    }

    private class TenTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    private class CurDateTextListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    private class EndDateTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    private class DayTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }
}
