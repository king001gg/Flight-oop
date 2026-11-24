package oop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 订票订单实体类。
 * <p>一个订单对应一位客户在某个航班上的一次订票，包含订单编号、
 * 客户资料（姓名、证件号）、订票数量、下单时间、成交单价与状态。</p>
 */
public class Order {

    /** 订单状态 */
    public enum Status {
        BOOKED("已订"),
        REFUNDED("已退");

        private final String text;

        Status(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Status fromText(String text) {
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(text) || s.text.equals(text)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("未知订单状态：" + text);
        }
    }

    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String orderId;
    private String flightId;
    private String customerName;
    private String customerId;
    private int quantity;
    private LocalDateTime orderTime;
    private double unitPrice;
    private Status status;
    private LocalDateTime refundTime;

    public Order() {
    }

    public Order(String orderId, String flightId, String customerName, String customerId,
                 int quantity, LocalDateTime orderTime, double unitPrice,
                 Status status, LocalDateTime refundTime) {
        this.orderId = orderId;
        this.flightId = flightId;
        this.customerName = customerName;
        this.customerId = customerId;
        this.quantity = quantity;
        this.orderTime = orderTime;
        this.unitPrice = unitPrice;
        this.status = status;
        this.refundTime = refundTime;
    }

    // ── getter / setter ─────────────────────────────────────────

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }

    // ── 派生属性 ────────────────────────────────────────────────

    /** 订单总金额 = 成交单价 × 数量 */
    public double getAmount() {
        return Flight.round2(unitPrice * quantity);
    }

    public boolean isRefunded() {
        return status == Status.REFUNDED;
    }

    public String getOrderTimeText() {
        return orderTime == null ? "" : orderTime.format(DATE_TIME_FORMAT);
    }

    public String getRefundTimeText() {
        return refundTime == null ? "" : refundTime.format(DATE_TIME_FORMAT);
    }

    public String getStatusText() {
        return status == null ? "" : status.getText();
    }

    // ── 文件序列化 ──────────────────────────────────────────────

    /** 序列化为 orders.dat 中的一行（9 个字段，:: 分隔） */
    public String toDataLine() {
        return String.join("::",
                orderId,
                flightId,
                customerName,
                customerId,
                String.valueOf(quantity),
                getOrderTimeText(),
                String.valueOf(unitPrice),
                status == null ? "" : status.name(),
                getRefundTimeText());
    }

    /** 从 orders.dat 的一行解析订单 */
    public static Order fromDataLine(String line) {
        String[] parts = line.split("::", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException("字段数应为 9，实际为 " + parts.length);
        }
        Order o = new Order();
        o.setOrderId(requireText(parts[0], "订单编号"));
        o.setFlightId(requireText(parts[1], "航班编号"));
        o.setCustomerName(requireText(parts[2], "客户姓名"));
        o.setCustomerId(requireText(parts[3], "证件号"));
        o.setQuantity(parsePositiveInt(parts[4], "订票数量"));
        o.setOrderTime(parseDateTime(parts[5], "下单时间"));
        o.setUnitPrice(parseNonNegativeDouble(parts[6], "成交单价"));
        o.setStatus(Status.fromText(parts[7].trim()));
        String refundText = parts[8].trim();
        o.setRefundTime(refundText.isEmpty() ? null : parseDateTime(refundText, "退票时间"));
        return o;
    }

    // ── 解析辅助 ────────────────────────────────────────────────

    private static String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return text.trim();
    }

    private static int parsePositiveInt(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + "必须大于 0：" + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是整数：" + text);
        }
    }

    private static double parseNonNegativeDouble(String text, String fieldName) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + "不能为负数：" + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是数字：" + text);
        }
    }

    private static LocalDateTime parseDateTime(String text, String fieldName) {
        String normalized = text.trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + "格式应为 yyyy-MM-dd HH:mm:ss：" + text);
        }
    }
}
