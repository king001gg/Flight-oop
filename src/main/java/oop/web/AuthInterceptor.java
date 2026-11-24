package oop.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oop.AuthService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 校验 Bearer Token；/api/admin/** 额外要求 ADMIN 角色。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTRIBUTE = "authSession";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        AuthService.Session session = authService.validate(extractToken(request));
        if (session == null) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        if (request.getRequestURI().startsWith("/api/admin/") && !session.isAdmin()) {
            writeError(response, 403, "无权限访问该功能");
            return false;
        }
        request.setAttribute(SESSION_ATTRIBUTE, session);
        return true;
    }

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(status, message, null));
    }
}
