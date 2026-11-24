package oop;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * users.dat 的加载、保存与 CRUD。
 * <p>密码只保存 BCrypt 哈希；默认管理员和演示客户在首次启动时生成。</p>
 */
public class UserStore {

    private final Path file;
    private final PasswordEncoder encoder;
    private final List<User> users = new ArrayList<>();
    private final Map<String, User> byUsername = new HashMap<>();

    public UserStore(String dataDir, PasswordEncoder encoder) {
        this(Paths.get(dataDir), encoder);
    }

    public UserStore(Path dataDir, PasswordEncoder encoder) {
        this.file = dataDir.toAbsolutePath().normalize().resolve("users.dat");
        this.encoder = encoder;
    }

    public synchronized List<String> load() {
        users.clear();
        byUsername.clear();
        List<String> warnings = new ArrayList<>();
        if (!Files.exists(file)) {
            warnings.add("未找到 users.dat，将创建默认账号");
            return warnings;
        }
        List<String> lines;
        try {
            lines = DataFiles.readLines(file);
        } catch (IOException e) {
            warnings.add("读取 users.dat 失败：" + e.getMessage());
            return warnings;
        }
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                User user = User.fromDataLine(line);
                if (byUsername.containsKey(user.getUsername())) {
                    warnings.add("users.dat 第 " + lineNo + " 行：用户名 "
                            + user.getUsername() + " 重复，已忽略");
                    continue;
                }
                users.add(user);
                byUsername.put(user.getUsername(), user);
            } catch (RuntimeException e) {
                warnings.add("users.dat 第 " + lineNo + " 行：" + e.getMessage());
            }
        }
        sortAndRebuild();
        return warnings;
    }

    public synchronized void save() throws FlightSystemException {
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(user.toDataLine());
        }
        try {
            DataFiles.writeLines(file, lines);
        } catch (IOException e) {
            throw new FlightSystemException("保存 users.dat 失败：" + e.getMessage(), e);
        }
    }

    public synchronized boolean isEmpty() {
        return users.isEmpty();
    }

    public synchronized List<User> getAll() {
        return new ArrayList<>(users);
    }

    public synchronized User find(String username) {
        return byUsername.get(normalize(username));
    }

    public synchronized int countAdmins() {
        int count = 0;
        for (User user : users) {
            if (user.isAdmin()) {
                count++;
            }
        }
        return count;
    }

    public synchronized void add(User user) throws FlightSystemException {
        if (user == null) {
            throw new FlightSystemException("用户信息不能为空");
        }
        try {
            User.validate(user);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
        String username = user.getUsername().trim();
        user.setUsername(username);
        if (byUsername.containsKey(username)) {
            throw new FlightSystemException("用户名「" + username + "」已存在");
        }
        users.add(user);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            users.remove(user);
            sortAndRebuild();
            throw e;
        }
    }

    public synchronized void update(String username, User updated) throws FlightSystemException {
        String name = normalize(username);
        User existing = byUsername.get(name);
        if (existing == null) {
            throw new FlightSystemException("用户不存在：" + username);
        }
        if (updated == null) {
            throw new FlightSystemException("用户信息不能为空");
        }
        if (!name.equals(normalize(updated.getUsername()))) {
            throw new FlightSystemException("用户名不能修改");
        }
        updated.setPasswordHash(existing.getPasswordHash());
        try {
            User.validate(updated);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
        users.remove(existing);
        users.add(updated);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            users.remove(updated);
            users.add(existing);
            sortAndRebuild();
            throw e;
        }
    }

    public synchronized void changePassword(String username, String rawPassword)
            throws FlightSystemException {
        String name = normalize(username);
        User user = byUsername.get(name);
        if (user == null) {
            throw new FlightSystemException("用户不存在：" + username);
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new FlightSystemException("密码长度不能少于 6 位");
        }
        String oldHash = user.getPasswordHash();
        user.setPasswordHash(encoder.encode(rawPassword));
        try {
            save();
        } catch (FlightSystemException e) {
            user.setPasswordHash(oldHash);
            throw e;
        }
    }

    public synchronized void delete(String username) throws FlightSystemException {
        String name = normalize(username);
        User existing = byUsername.get(name);
        if (existing == null) {
            throw new FlightSystemException("用户不存在：" + username);
        }
        users.remove(existing);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            users.add(existing);
            sortAndRebuild();
            throw e;
        }
    }

    /** 首次启动生成默认账号：admin/admin123、zhangwei/123456 */
    public synchronized void seedDefaults() throws FlightSystemException {
        if (!users.isEmpty()) {
            return;
        }
        User admin = new User("admin", encoder.encode("admin123"),
                User.Role.ADMIN, "系统管理员", "");
        User customer = new User("zhangwei", encoder.encode("123456"),
                User.Role.CUSTOMER, "张伟", "450102199001011234");
        users.add(admin);
        users.add(customer);
        sortAndRebuild();
        save();
    }

    private void sortAndRebuild() {
        users.sort(Comparator.comparing(User::getUsername));
        byUsername.clear();
        for (User user : users) {
            byUsername.put(user.getUsername(), user);
        }
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
