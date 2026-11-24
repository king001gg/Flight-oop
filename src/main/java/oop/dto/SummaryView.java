package oop.dto;

public record SummaryView(
        int flightCount,
        int orderCount,
        int activeOrderCount,
        int passengerCount,
        double revenue) {
}
