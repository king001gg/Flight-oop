package oop.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record FareRequest(
        @DecimalMin(value = "0", message = "票价不能为负数") double price,
        @DecimalMin(value = "0.01", message = "折扣必须大于 0")
        @DecimalMax(value = "1", message = "折扣不能大于 1") double discount) {
}
