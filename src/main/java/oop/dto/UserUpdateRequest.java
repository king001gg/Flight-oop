package oop.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(message = "角色不能为空") String role,
        @NotBlank(message = "显示名不能为空") String displayName,
        String customerId) {
}
