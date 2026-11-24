package oop;

/**
 * 系统登录用户。
 * <p>密码只保存 BCrypt 哈希；客户角色通过 customerId 关联客户资料。</p>
 */
public class User {

    public enum Role {
        ADMIN("管理员"),
        CUSTOMER("客户");

        private final String text;

        Role(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Role fromText(String value) {
            for (Role role : values()) {
                if (role.name().equalsIgnoreCase(value) || role.getText().equals(value)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("未知用户角色：" + value);
        }
    }

    private String username;
    private String passwordHash;
    private Role role;
    private String displayName;
    private String customerId = "";

    public User() {
    }

    public User(String username, String passwordHash, Role role,
                String displayName, String customerId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public String getRoleText() {
        return role == null ? "" : role.getText();
    }

    /** 序列化为 users.dat 的一行（5 字段） */
    public String toDataLine() {
        return String.join("::",
                username,
                passwordHash,
                role == null ? "" : role.name(),
                displayName,
                customerId == null ? "" : customerId);
    }

    /** 从 users.dat 的一行解析用户 */
    public static User fromDataLine(String line) {
        String[] parts = line.split("::", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("字段数应为 5，实际为 " + parts.length);
        }
        User user = new User();
        user.setUsername(requireText(parts[0], "用户名"));
        user.setPasswordHash(requireText(parts[1], "密码哈希"));
        user.setRole(Role.fromText(parts[2].trim()));
        user.setDisplayName(requireText(parts[3], "显示名"));
        user.setCustomerId(parts[4].trim());
        validate(user);
        return user;
    }

    public static void validate(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        requireText(user.getUsername(), "用户名");
        requireText(user.getPasswordHash(), "密码哈希");
        if (user.getRole() == null) {
            throw new IllegalArgumentException("用户角色不能为空");
        }
        requireText(user.getDisplayName(), "显示名");
        checkDelimiter(user.getUsername(), "用户名");
        checkDelimiter(user.getPasswordHash(), "密码哈希");
        checkDelimiter(user.getDisplayName(), "显示名");
        checkDelimiter(user.getCustomerId(), "关联证件号");
    }

    private static String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return text.trim();
    }

    private static void checkDelimiter(String value, String fieldName) {
        if (value != null && value.contains("::")) {
            throw new IllegalArgumentException(fieldName + "不能包含分隔符「::」");
        }
    }
}
