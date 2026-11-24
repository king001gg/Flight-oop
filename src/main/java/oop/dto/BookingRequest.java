package oop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingRequest(
        @NotBlank(message = "航班号不能为空") String flightId,
        @Min(value = 1, message = "订票数量必须大于 0") int quantity,
        String customerId) {
}
