package oop.dto;

import oop.Flight;
import oop.Order;

public record OrderView(
        String orderId,
        String flightId,
        String flightDate,
        String route,
        String customerName,
        String customerId,
        int quantity,
        String orderTime,
        double unitPrice,
        double amount,
        String status,
        String statusText,
        String refundTime) {

    public static OrderView from(Order order, Flight flight) {
        return new OrderView(
                order.getOrderId(),
                order.getFlightId(),
                flight == null ? "" : flight.getDateText(),
                flight == null ? "未知" : flight.getRoute(),
                order.getCustomerName(),
                order.getCustomerId(),
                order.getQuantity(),
                order.getOrderTimeText(),
                order.getUnitPrice(),
                order.getAmount(),
                order.getStatus() == null ? "" : order.getStatus().name(),
                order.getStatusText(),
                order.getRefundTimeText());
    }
}
