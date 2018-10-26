package com.bop.tiebei;

import java.text.DecimalFormat;

/**
 * @author lq.zeng
 * @date 2018/8/14
 */

public class CalculatorImp implements ICalculator {

    private double value1, value2;
    private DecimalFormat df = new DecimalFormat("0.00");

    public CalculatorImp(double value1, double value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public String add() {
        return df.format(value1 + value2);
    }

    @Override
    public String minus() {
        return df.format(value1 - value2);
    }

    @Override
    public String mul() {
        return df.format(value1 * value2);
    }

    @Override
    public String div() {
        return df.format(value1 / value2);
    }
}
