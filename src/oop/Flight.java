package oop;
/**
 * 航班实体类
 * 封装航班所有属性 + getter/setter
 */
public class Flight {
    // 题目要求的所有字段
    private String flightId;     // 航班编号
    private String takeOffTime; // 起飞时间
    private String duration;     // 飞行时长
    private String startCity;    // 出发城市
    private String endCity;      // 到达城市
    private String planeModel;   // 飞机型号
    private double price;        // 票价
    private double onTimeRate;   // 准点率(%)
    private String airline;      // 所属航空公司

    // 无参构造
    public Flight() {}

    // 全参构造
    public Flight(String flightId, String takeOffTime, String duration,
                  String startCity, String endCity, String planeModel,
                  double price, double onTimeRate, String airline) {
        this.flightId = flightId;
        this.takeOffTime = takeOffTime;
        this.duration = duration;
        this.startCity = startCity;
        this.endCity = endCity;
        this.planeModel = planeModel;
        this.price = price;
        this.onTimeRate = onTimeRate;
        this.airline = airline;
    }

    // Getter & Setter
    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getTakeOffTime() {
        return takeOffTime;
    }

    public void setTakeOffTime(String takeOffTime) {
        this.takeOffTime = takeOffTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStartCity() {
        return startCity;
    }

    public void setStartCity(String startCity) {
        this.startCity = startCity;
    }

    public String getEndCity() {
        return endCity;
    }

    public void setEndCity(String endCity) {
        this.endCity = endCity;
    }

    public String getPlaneModel() {
        return planeModel;
    }

    public void setPlaneModel(String planeModel) {
        this.planeModel = planeModel;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(double onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }
}