package oop;

/**
 * 某航线在指定时间段内的汇总统计结果。
 */
public class RouteStatistics {

    private final int flightCount;
    private final int totalSeats;
    private final int passengers;

    public RouteStatistics(int flightCount, int totalSeats, int passengers) {
        this.flightCount = flightCount;
        this.totalSeats = totalSeats;
        this.passengers = passengers;
    }

    /** 飞行次数（航班记录条数） */
    public int getFlightCount() {
        return flightCount;
    }

    /** 可提供的总座位数 */
    public int getTotalSeats() {
        return totalSeats;
    }

    /** 载客人数（有效订单的订票数量之和） */
    public int getPassengers() {
        return passengers;
    }

    /** 客座率（%） */
    public double getOccupancyRate() {
        return totalSeats == 0 ? 0 : passengers * 100.0 / totalSeats;
    }
}
