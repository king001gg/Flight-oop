package oop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import oop.AuthService;
import oop.CustomerStore;
import oop.FlightSystemException;
import oop.User;
import oop.UserStore;
import oop.dto.PasswordResetRequest;
import oop.dto.UserCreateRequest;
import oop.dto.UserUpdateRequest;
import oop.dto.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserStore userStore;
    private final CustomerStore customerStore;
    private final AuthService authService;
    private final PasswordEncoder encoder;

    public AdminUserController(UserStore userStore, CustomerStore customerStore,
                               AuthService authService, PasswordEncoder encoder) {
        this.userStore = userStore;
        this.customerStore = customerStore;
        this.authService = authService;
        this.encoder = encoder;
    }

    @GetMapping
    public ApiResponse<List<UserView>> list() {
        return ApiResponse.ok(userStore.getAll().stream()
                .map(UserView::from)
                .toList());
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody UserCreateRequest body)
            throws FlightSystemException {
        User.Role role = parseRole(body.role());
        String customerId = role == User.Role.CUSTOMER
                ? requireCustomer(body.customerId())
                : "";
        User user = new User(
                body.username().trim(),
                encoder.encode(body.password()),
                role,
                body.displayName().trim(),
                customerId);
        userStore.add(user);
        return ApiResponse.ok(UserView.from(userStore.find(user.getUsername())));
    }

    @PutMapping("/{username}")
    public ApiResponse<UserView> update(@PathVariable String username,
                                        @Valid @RequestBody UserUpdateRequest body)
            throws FlightSystemException {
        User existing = userStore.find(username);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在：" + username);
        }
        User.Role role = parseRole(body.role());
        String customerId = role == User.Role.CUSTOMER
                ? requireCustomer(body.customerId())
                : "";

        if (existing.isAdmin() && role != User.Role.ADMIN
                && userStore.countAdmins() <= 1) {
            throw new FlightSystemException("不能取消最后一个管理员的角色");
        }

        boolean roleChanged = existing.getRole() != role;
        boolean customerChanged = !same(existing.getCustomerId(), customerId);

        User updated = new User(existing.getUsername(), existing.getPasswordHash(),
                role, body.displayName().trim(), customerId);
        userStore.update(username, updated);
        if (roleChanged || customerChanged) {
            authService.invalidateUser(username);
        }
        return ApiResponse.ok(UserView.from(userStore.find(username)));
    }

    @PutMapping("/{username}/password")
    public ApiResponse<Void> resetPassword(@PathVariable String username,
                                           @Valid @RequestBody PasswordResetRequest body)
            throws FlightSystemException {
        if (userStore.find(username) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在：" + username);
        }
        userStore.changePassword(username, body.password());
        authService.invalidateUser(username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{username}")
    public ApiResponse<Void> delete(@PathVariable String username,
                                    HttpServletRequest request)
            throws FlightSystemException {
        AuthService.Session session = (AuthService.Session)
                request.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);
        User user = userStore.find(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在：" + username);
        }
        if (user.getUsername().equals(session.username())) {
            throw new FlightSystemException("不能删除当前登录账号");
        }
        if (user.isAdmin() && userStore.countAdmins() <= 1) {
            throw new FlightSystemException("不能删除最后一个管理员");
        }
        userStore.delete(username);
        authService.invalidateUser(username);
        return ApiResponse.ok(null);
    }

    private String requireCustomer(String customerId) throws FlightSystemException {
        String id = normalize(customerId);
        if (id.isEmpty()) {
            throw new FlightSystemException("客户账号必须关联证件号");
        }
        if (customerStore.find(id) == null) {
            throw new FlightSystemException("关联的客户不存在：" + id);
        }
        return id;
    }

    private static User.Role parseRole(String role) throws FlightSystemException {
        try {
            return User.Role.fromText(role);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
    }

    private static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
