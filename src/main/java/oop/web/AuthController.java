package oop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import oop.AuthService;
import oop.FlightSystemException;
import oop.User;
import oop.UserStore;
import oop.dto.LoginRequest;
import oop.dto.LoginResponse;
import oop.dto.RegisterRequest;
import oop.dto.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserStore userStore;

    public AuthController(AuthService authService, UserStore userStore) {
        this.authService = authService;
        this.userStore = userStore;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request)
            throws FlightSystemException {
        AuthService.Session session = authService.login(request.username(), request.password());
        User user = userStore.find(session.username());
        return ApiResponse.ok(new LoginResponse(session.token(), UserView.from(user)));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request)
            throws FlightSystemException {
        AuthService.Session session = authService.register(
                request.username(), request.password(), request.name(),
                request.idNumber(), request.phone(), request.email());
        User user = userStore.find(session.username());
        return ApiResponse.ok(new LoginResponse(session.token(), UserView.from(user)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(AuthInterceptor.extractToken(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(HttpServletRequest request) throws FlightSystemException {
        AuthService.Session session = (AuthService.Session)
                request.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);
        User user = userStore.find(session.username());
        if (user == null) {
            throw new FlightSystemException("用户不存在");
        }
        return ApiResponse.ok(UserView.from(user));
    }
}
