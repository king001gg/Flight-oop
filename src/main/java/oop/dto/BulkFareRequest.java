package oop.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record BulkFareRequest(
        @NotBlank(message = "出发城市不能为空") String startCity,
        @NotBlank(message = "到达城市不能为空") String endCity,
        @DecimalMin(value = "0.01", message = "折扣必须大于 0")
        @DecimalMax(value = "1", message = "折扣不能大于 1") double discount) {
}
