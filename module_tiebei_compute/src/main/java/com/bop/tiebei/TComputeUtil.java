package com.bop.tiebei;

import com.apkfuns.logutils.LogUtils;
import com.king.android.res.util.ConstUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;

import google.architecture.common.util.TimeUtils;

/**
 * @author lq.zeng
 * @date 2018/8/7
 */

public final class TComputeUtil {

    /**
     * 获取贴现金额<p>
     * 贴现金额 = [票面金额 - （票面金额*年化率/360*（到期日-当前日期+调整天数）+票面金额/100000*每十万扣款）]
     * @param ticketMoney 票面金额
     * @param yearRate 年化率
     * @param dates 日期换算
     * @param deductMoney 扣款
     * @return String
     */
    public static double getDiscountAmount(double ticketMoney, double yearRate, long dates, double deductMoney) {
        double discountAmount = ticketMoney - (ticketMoney * yearRate / 360 * dates + ticketMoney / 100000 * deductMoney);
        return discountAmount;
    }

    /**
     * 到期日-当前日期+调整天数
      * @param endDate 到期日
     * @param curDate 当前日期
     * @param adjustmentDate 调整天数
     * @return int
     */
    public static long getDates(String endDate, String curDate, int adjustmentDate) {
        long fitDate = TimeUtils.getTimeSpan(endDate, curDate, ConstUtils.TimeUnit.DAY, "yyyy-MM-dd"); //时间差
        return fitDate + adjustmentDate;
    }

    public static String[] getAdjustmentDate(int type, String curDate) {
        String endDate[] = new String[2];
        switch (type) {
            case 0: //一年电
                endDate = timeCompute(Calendar.YEAR, curDate, 1);
                break;
            case 1: //半年电
            case 2: //半年纸
                endDate = timeCompute(Calendar.MONTH, curDate, 6);
                break;
        }
        return endDate;
    }

    public static String[] timeCompute(int type, String curDate, int dateNum) {
        String[] date = new String[2];
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(TimeUtils.string2Date(curDate, "yyyy-MM-dd"));
        switch (type) {
            case Calendar.YEAR:
                calendar.add(Calendar.YEAR, dateNum);
                break;
            case Calendar.MONTH:
                calendar.add(Calendar.MONTH, dateNum);
                break;
        }
        long afterAYear = calendar.getTimeInMillis();
        date[0] = TimeUtils.millis2String(afterAYear, "yyyy-MM-dd");
        date[1] = TimeUtils.getWeek(afterAYear);
        return date;
    }

    public static String getAdjustmentDayNum(boolean isPaper, String endData) {
        String adjustmentDayNum = isPaper ? "3" : "0";
        int weekIndex = TimeUtils.getWeekIndex(endData, "yyyy-MM-dd");
        LogUtils.tag("zlq").e("weekIndex = " + weekIndex);
        switch (weekIndex) {
            case 1: //周日
                adjustmentDayNum = isPaper ? "4" : "1";
                break;
            case 7: //周六
                adjustmentDayNum = isPaper ? "5" : "2";
                break;
            default:
                adjustmentDayNum = isPaper ? "3" : "0";
                break;
        }
        return adjustmentDayNum;
    }
}
