package oop.dto;

import oop.MonthStatistics;

public record MonthlyStatsView(
        String month,
        int soldTickets,
        int refundedTickets,
        int netTickets,
        double netRevenue) {

    public static MonthlyStatsView from(MonthStatistics statistics) {
        return new MonthlyStatsView(
                statistics.getMonthText(),
                statistics.getSoldTickets(),
                statistics.getRefundedTickets(),
                statistics.getNetTickets(),
                statistics.getNetRevenue());
    }
}
