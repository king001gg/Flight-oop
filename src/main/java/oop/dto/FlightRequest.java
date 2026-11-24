package oop.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import oop.Flight;

import java.time.LocalDate;
import java.time.LocalTime;

public record FlightRequest(
        @NotBlank(message = "航班编号不能为空") String flightId,
        @NotNull(message = "执飞日期不能为空") LocalDate date,
        @NotNull(message = "起飞时间不能为空") LocalTime takeOffTime,
        @Min(value = 1, message = "飞行时长必须大于 0") int durationMinutes,
        @NotBlank(message = "出发城市不能为空") String startCity,
        @NotBlank(message = "到达城市不能为空") String endCity,
        @NotBlank(message = "飞机型号不能为空") String planeModel,
        @DecimalMin(value = "0", message = "票价不能为负数") double price,
        @DecimalMin(value = "0.01", message = "折扣必须大于 0")
        @DecimalMax(value = "1", message = "折扣不能大于 1") double discount,
        @Min(value = 1, message = "总座位数必须大于 0") int totalSeats,
        @NotBlank(message = "航空公司不能为空") String airline,
        @DecimalMin(value = "0", message = "准点率不能为负数")
        @DecimalMax(value = "100", message = "准点率不能大于 100") double onTimeRate) {

    public Flight toFlight() {
        return new Flight(flightId.trim(), date, takeOffTime, durationMinutes,
                startCity.trim(), endCity.trim(), planeModel.trim(),
                price, discount, totalSeats, airline.trim(), onTimeRate);
    }
}
