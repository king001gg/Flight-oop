package oop.dto;

import jakarta.validation.constraints.NotBlank;
import oop.Customer;

import java.time.LocalDateTime;

public record CustomerRequest(
        @NotBlank(message = "证件号不能为空") String customerId,
        @NotBlank(message = "姓名不能为空") String name,
        String phone,
        String email,
        @NotBlank(message = "客户等级不能为空") String level) {

    public Customer toCustomer(LocalDateTime createdAt) {
        return new Customer(
                customerId.trim(),
                name.trim(),
                phone == null ? "" : phone.trim(),
                email == null ? "" : email.trim(),
                Customer.Level.fromText(level.trim()),
                createdAt);
    }
}
