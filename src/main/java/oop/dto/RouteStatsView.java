package oop.dto;

import oop.Flight;
import oop.RouteStatistics;

public record RouteStatsView(
        int flightCount,
        int totalSeats,
        int passengers,
        double occupancyRate) {

    public static RouteStatsView from(RouteStatistics statistics) {
        return new RouteStatsView(
                statistics.getFlightCount(),
                statistics.getTotalSeats(),
                statistics.getPassengers(),
                Flight.round2(statistics.getOccupancyRate()));
    }
}
