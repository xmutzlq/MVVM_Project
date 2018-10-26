package com.bop.tiebei;

/**
 * @author lq.zeng
 * @date 2018/8/14
 */

public class CalculatorFactory {
    public static String compute(String type, double value1, double value2) {
        CalculatorImp calculatorImp = new CalculatorImp(value1, value2);
        switch (type) {
            case "+":
                return calculatorImp.add();
            case "-":
                return calculatorImp.minus();
            case "*":
                return calculatorImp.mul();
            case "/":
                return calculatorImp.div();
        }
        return "0";
    }
}
