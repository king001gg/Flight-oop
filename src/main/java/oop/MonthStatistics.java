package oop;

import java.time.YearMonth;

/**
 * 单个自然月的机票销售汇总。
 */
public class MonthStatistics {

    private final YearMonth month;
    private final int soldTickets;
    private final int refundedTickets;
    private final double netRevenue;

    public MonthStatistics(YearMonth month, int soldTickets, int refundedTickets, double netRevenue) {
        this.month = month;
        this.soldTickets = soldTickets;
        this.refundedTickets = refundedTickets;
        this.netRevenue = netRevenue;
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthText() {
        return month.toString();
    }

    /** 当月下单的机票张数（含之后退票的） */
    public int getSoldTickets() {
        return soldTickets;
    }

    /** 当月的退票张数（按下单时间归档的退票不计入此处） */
    public int getRefundedTickets() {
        return refundedTickets;
    }

    /** 净售票 = 当月售票 − 当月退票 */
    public int getNetTickets() {
        return soldTickets - refundedTickets;
    }

    /** 净销售额 = 当月售票金额 − 当月退票金额 */
    public double getNetRevenue() {
        return netRevenue;
    }
}
