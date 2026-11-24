package oop.dto;

import oop.Customer;

public record CustomerView(
        String customerId,
        String name,
        String phone,
        String email,
        String level,
        String levelText,
        String createdAt) {

    public static CustomerView from(Customer customer) {
        return new CustomerView(
                customer.getCustomerId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getLevel() == null ? "" : customer.getLevel().name(),
                customer.getLevelText(),
                customer.getCreatedAtText());
    }
}
