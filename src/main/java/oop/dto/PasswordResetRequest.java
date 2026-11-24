package oop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6~64 位")
        String password) {
}
