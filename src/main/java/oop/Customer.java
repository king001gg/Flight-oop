package oop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 客户资料。
 * <p>以证件号（customerId）为主键；订单中保留客户姓名/证件号快照，
 * 因此修改客户资料不会改写历史订单。</p>
 */
public class Customer {

    /** 会员等级 */
    public enum Level {
        NORMAL("普通"),
        SILVER("银卡"),
        GOLD("金卡");

        private final String text;

        Level(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Level fromText(String value) {
            for (Level level : values()) {
                if (level.name().equalsIgnoreCase(value) || level.getText().equals(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("未知客户等级：" + value);
        }
    }

    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String customerId;
    private String name;
    private String phone = "";
    private String email = "";
    private Level level = Level.NORMAL;
    private LocalDateTime createdAt;

    public Customer() {
    }

    public Customer(String customerId, String name, String phone, String email,
                    Level level, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.level = level;
        this.createdAt = createdAt;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLevelText() {
        return level == null ? "" : level.getText();
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME_FORMAT);
    }

    /** 序列化为 customers.dat 的一行（6 字段） */
    public String toDataLine() {
        return String.join("::",
                customerId,
                name,
                phone == null ? "" : phone,
                email == null ? "" : email,
                level == null ? Level.NORMAL.name() : level.name(),
                getCreatedAtText());
    }

    /** 从 customers.dat 的一行解析客户 */
    public static Customer fromDataLine(String line) {
        String[] parts = line.split("::", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("字段数应为 6，实际为 " + parts.length);
        }
        Customer customer = new Customer();
        customer.setCustomerId(requireText(parts[0], "证件号"));
        customer.setName(requireText(parts[1], "姓名"));
        customer.setPhone(parts[2].trim());
        customer.setEmail(parts[3].trim());
        customer.setLevel(Level.fromText(parts[4].trim()));
        customer.setCreatedAt(parseDateTime(parts[5], "创建时间"));
        return customer;
    }

    public static void validate(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("客户信息不能为空");
        }
        requireText(customer.getCustomerId(), "证件号");
        requireText(customer.getName(), "姓名");
        if (customer.getLevel() == null) {
            throw new IllegalArgumentException("客户等级不能为空");
        }
        if (customer.getCreatedAt() == null) {
            throw new IllegalArgumentException("创建时间不能为空");
        }
        checkDelimiter(customer.getCustomerId(), "证件号");
        checkDelimiter(customer.getName(), "姓名");
        checkDelimiter(customer.getPhone(), "电话");
        checkDelimiter(customer.getEmail(), "邮箱");
    }

    private static String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return text.trim();
    }

    private static LocalDateTime parseDateTime(String text, String fieldName) {
        try {
            return LocalDateTime.parse(text.trim().replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + "格式应为 yyyy-MM-dd HH:mm:ss：" + text);
        }
    }

    private static void checkDelimiter(String value, String fieldName) {
        if (value != null && value.contains("::")) {
            throw new IllegalArgumentException(fieldName + "不能包含分隔符「::」");
        }
    }
}
