package com.bop.tiebei;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.bop.tiebei.databinding.FragmentCalculatorBinding;
import com.bop.tiebei.widget.raiflatbutton.RaiflatButton;
import com.king.android.res.config.ARouterPath;

import google.architecture.common.base.BaseFragment;
import google.architecture.coremodel.util.TextUtil;
import io.reactivex.Observable;

/**
 * @author lq.zeng
 * @date 2018/8/9
 */
@Route(path = ARouterPath.T_CalculatorFgt)
public class FragmentCalculator extends BaseFragment<FragmentCalculatorBinding> {

    private String calStr;
    private boolean useDisplay2;

    public static FragmentCalculator newInstance(String str) {
        FragmentCalculator fragmentCalculator = new FragmentCalculator();
        Bundle bundle = new Bundle();
        bundle.putString("extra_name", str);
        fragmentCalculator.setArguments(bundle);
        return fragmentCalculator;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_calculator;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitleName(R.string.t_title_calculator);

        //数字
        binding.layoutCalKeyboard.digit0.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit1.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit2.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit3.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit4.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit5.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit6.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit7.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit8.setOnClickListener(mNumberClick);
        binding.layoutCalKeyboard.digit9.setOnClickListener(mNumberClick);

        //加减乘除
        binding.layoutCalKeyboard.plus.setOnClickListener(mCalClick);
        binding.layoutCalKeyboard.minus.setOnClickListener(mCalClick);
        binding.layoutCalKeyboard.mul.setOnClickListener(mCalClick);
        binding.layoutCalKeyboard.div.setOnClickListener(mCalClick);

        //小数点
        binding.layoutCalKeyboard.dot.setOnClickListener(mNumberClick);

        //删除
        binding.del.setOnClickListener(v -> {
            if(haveResult()) {
                useDisplay2 = true;
            }
            disPlayResult();
            String display2Value = binding.calDisplay2.getText().toString();
            Observable.just(display2Value).all(s -> !TextUtil.isEmpty(s) && s.length() > 0).subscribe((aBoolean, throwable) -> {
                if(aBoolean) {
                    Observable.just(display2Value).filter(s -> !TextUtil.isEmpty(s) && s.length() > 0).subscribe(s -> {
                        if(s.length() == 1 && containCal(
                                String.valueOf(binding.calDisplay2.getText().toString().charAt(0)))) {
                            return;
                        }
                        binding.calDisplay2.setText(s.substring(0, s.length() - 1));
                    });
                } else {
                    if(useDisplay2) return;
                    String display1Value = binding.calDisplay1.getText().toString();
                    Observable.just(display1Value).filter(s -> !TextUtil.isEmpty(s) && s.length() > 0).subscribe(s -> {
                        if(s.length() == 1) {
                            binding.calDisplay1.setText("");
                        } else {
                            binding.calDisplay1.setText(s.substring(0, s.length() - 1));
                        }
                    });
                }
            });
        });

        //清除
        binding.clear.setOnClickListener(v -> {
            useDisplay2 = false;
            binding.calDisplay1.setText("");
            binding.calDisplay2.setText("");
            binding.calDisplay3.setText("");
            calStr = "";
        });

        //等于
        binding.layoutCalKeyboard.equal.setOnClickListener((View v) -> {
            String dis1Value = binding.calDisplay1.getText().toString();
            String dis2Value = binding.calDisplay2.getText().toString();
            if(!TextUtil.isEmpty(dis1Value) && !TextUtil.isEmpty(dis2Value)) {
                //过滤有符号没数字的情况
                if(!TextUtil.isEmpty(calStr) && dis2Value.length() > 1) {
                    double value1 = Double.valueOf(dis1Value);
                    double value2 = Double.valueOf(dis2Value.replace(String.valueOf(dis2Value.charAt(0)), ""));
                    String result = CalculatorFactory.compute(calStr, value1, value2);
                    binding.calDisplay3.setText(result);
                    useDisplay2 = true;
                }
            }
        });

        String extraValue = getArguments().getString("extra_name");
        binding.calDisplay1.setText(extraValue);
    }

    private View.OnClickListener mNumberClick = v -> {
        disPlayResult();
        if(useDisplay2) {
            addDisplay2(v);
        } else {
            String display2Value = binding.calDisplay2.getText().toString();
            Observable.just(display2Value).all(s -> !TextUtil.isEmpty(s) && s.length() > 0).subscribe((aBoolean, throwable) -> {
                if(aBoolean) {
                    addDisplay2(v);
                } else {
                    addDisplay1(v);
                }
            });
        }
    };

    private View.OnClickListener mCalClick = v -> {
        disPlayResult();
        String display2Value = binding.calDisplay1.getText().toString();
        Observable.just(display2Value).all(s -> !TextUtil.isEmpty(s) && s.length() > 0).subscribe((aBoolean, throwable) -> {
            if(aBoolean) {
                useDisplay2 = false;
                addDisplay2(v);
            }
        });
    };

    /**将结果赋予第一行显示**/
    private void disPlayResult() {
        if(haveResult()) {
            binding.calDisplay1.setText(binding.calDisplay3.getText().toString());
            binding.calDisplay3.setText("");
            binding.calDisplay2.setText("");
            calStr = "";
        }
    }

    private boolean haveResult() {
        String display3Value = binding.calDisplay3.getText().toString();
        return !TextUtil.isEmpty(display3Value) && display3Value.length() > 0;
    }

    private boolean containCal(String calStr) {
        return calStr.equals(binding.layoutCalKeyboard.plus.getText().toString())
                || calStr.equals(binding.layoutCalKeyboard.minus.getText().toString())
                || calStr.equals(binding.layoutCalKeyboard.mul.getText().toString())
                || calStr.equals(binding.layoutCalKeyboard.div.getText().toString());
    }

    private int checkDotMulti(String str, String strRes) {
        int n = 0;//计数器
        int index = 0;//指定字符的长度
        index = str.indexOf(strRes);
        while(index!=-1) {
            n++;
            index = str.indexOf(strRes,index+1);
        }
        return n;
    }

    private void addDisplay1(View v) {
        StringBuilder sb = new StringBuilder(binding.calDisplay1.getText().toString());
        Observable.just(v).filter(view -> view instanceof RaiflatButton).map(view -> {
            String value = ((RaiflatButton)view).getText().toString();
            if(!(value.equals(".") && checkDotMulti(sb.toString(), ".") == 1)) sb.append(value); return sb;}
        ).subscribe(stringBuilder -> binding.calDisplay1.setText(stringBuilder.toString()));
    }

    private void addDisplay2(View v) {
        StringBuilder sb = new StringBuilder(binding.calDisplay2.getText().toString());
        Observable.just(v).filter(view -> view instanceof RaiflatButton).map(view -> {
            String value = ((RaiflatButton)view).getText().toString();
            if(value.equals(".") && checkDotMulti(sb.toString(), ".") == 1) return sb;
            boolean isCalStr = false;
            if (value.equals(binding.layoutCalKeyboard.plus.getText().toString())) {
                isCalStr = true;
                calStr = "+";
            } else if(value.equals(binding.layoutCalKeyboard.minus.getText().toString())) {
                isCalStr = true;
                calStr = "-";
            } else if(value.equals(binding.layoutCalKeyboard.mul.getText().toString())) {
                isCalStr = true;
                calStr = "*";
            } else if(value.equals(binding.layoutCalKeyboard.div.getText().toString())) {
                isCalStr = true;
                calStr = "/";
            }
            if(!TextUtil.isEmpty(sb.toString()) && sb.toString().length() > 0) {
                String firstStr = String.valueOf(sb.toString().charAt(0));
                if(isCalStr) {
                    if (containCal(firstStr)) {
                        return sb.replace(0, 1, value);
                    } else  {
                        return value + sb.toString();
                    }
                }
            }
            return sb.append(value);
        }).subscribe(stringBuilder -> binding.calDisplay2.setText(stringBuilder.toString()));
    }
}
