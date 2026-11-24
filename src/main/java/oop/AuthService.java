package oop;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录、注册与会话管理。
 * <p>Token 只保存在内存中，默认 8 小时过期；服务重启后需要重新登录。</p>
 */
public class AuthService {

    private static final Duration TOKEN_TTL = Duration.ofHours(8);

    private final UserStore userStore;
    private final CustomerStore customerStore;
    private final PasswordEncoder encoder;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserStore userStore, CustomerStore customerStore, PasswordEncoder encoder) {
        this.userStore = userStore;
        this.customerStore = customerStore;
        this.encoder = encoder;
    }

    public record Session(String token, String username, User.Role role,
                          String displayName, String customerId, Instant expiresAt) {
        public boolean isAdmin() {
            return role == User.Role.ADMIN;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public synchronized Session login(String username, String password) throws FlightSystemException {
        User user = userStore.find(username);
        if (user == null || password == null
                || !encoder.matches(password, user.getPasswordHash())) {
            throw new FlightSystemException("用户名或密码错误");
        }
        return createSession(user);
    }

    /**
     * 注册客户账号：同时创建客户资料（证件号为主键）和登录用户。
     */
    public synchronized Session register(String username, String password, String name,
                                         String idNumber, String phone, String email)
            throws FlightSystemException {
        String normalizedUsername = normalize(username);
        String normalizedName = normalize(name);
        String normalizedId = normalize(idNumber);
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 30) {
            throw new FlightSystemException("用户名长度应为 3~30 位");
        }
        if (password == null || password.length() < 6) {
            throw new FlightSystemException("密码长度不能少于 6 位");
        }
        if (normalizedName.isEmpty()) {
            throw new FlightSystemException("姓名不能为空");
        }
        if (normalizedId.isEmpty()) {
            throw new FlightSystemException("证件号不能为空");
        }
        if (userStore.find(normalizedUsername) != null) {
            throw new FlightSystemException("用户名「" + normalizedUsername + "」已存在");
        }
        if (customerStore.find(normalizedId) != null) {
            throw new FlightSystemException("证件号「" + normalizedId + "」已注册");
        }

        Customer customer = new Customer(normalizedId, normalizedName,
                normalize(phone), normalize(email),
                Customer.Level.NORMAL, LocalDateTime.now());
        customerStore.add(customer);
        try {
            User user = new User(normalizedUsername, encoder.encode(password),
                    User.Role.CUSTOMER, normalizedName, normalizedId);
            userStore.add(user);
            return createSession(user);
        } catch (FlightSystemException e) {
            try {
                customerStore.delete(normalizedId);
            } catch (FlightSystemException ignored) {
                // 回滚失败时保留客户资料，下一次注册仍会因证件号重复被拒绝
            }
            throw e;
        }
    }

    /** 校验 Token；无效或过期返回 null */
    public Session validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.isExpired()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** 用户被删除、改角色或重置密码后，使该用户的所有会话失效 */
    public void invalidateUser(String username) {
        if (username == null) {
            return;
        }
        sessions.values().removeIf(session -> session.username().equals(username));
    }

    private Session createSession(User user) {
        String token = UUID.randomUUID().toString();
        Session session = new Session(
                token,
                user.getUsername(),
                user.getRole(),
                user.getDisplayName(),
                user.getCustomerId() == null ? "" : user.getCustomerId(),
                Instant.now().plus(TOKEN_TTL));
        sessions.put(token, session);
        return session;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
