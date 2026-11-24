package oop.dto;

import oop.Flight;

public record FlightView(
        String flightId,
        String date,
        String takeOffTime,
        String arrivalTime,
        int durationMinutes,
        String startCity,
        String endCity,
        String route,
        String planeModel,
        double price,
        double discount,
        double actualPrice,
        int totalSeats,
        int bookedSeats,
        int remainingSeats,
        boolean full,
        String airline,
        double onTimeRate) {

    public static FlightView from(Flight flight) {
        return new FlightView(
                flight.getFlightId(),
                flight.getDateText(),
                flight.getTimeText(),
                flight.getArrivalTimeText(),
                flight.getDurationMinutes(),
                flight.getStartCity(),
                flight.getEndCity(),
                flight.getRoute(),
                flight.getPlaneModel(),
                flight.getPrice(),
                flight.getDiscount(),
                flight.getActualPrice(),
                flight.getTotalSeats(),
                flight.getBookedSeats(),
                flight.getRemainingSeats(),
                flight.isFull(),
                flight.getAirline(),
                flight.getOnTimeRate());
    }
}
