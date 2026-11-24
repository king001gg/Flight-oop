package oop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "用户名长度应为 3~30 位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6~64 位")
        String password,

        @NotBlank(message = "角色不能为空") String role,
        @NotBlank(message = "显示名不能为空") String displayName,
        String customerId) {
}
