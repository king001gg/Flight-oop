package oop.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：用于确认后端与静态资源服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("app", "flight-booking-web");
        result.put("time", LocalDateTime.now().toString());
        return result;
    }
}
