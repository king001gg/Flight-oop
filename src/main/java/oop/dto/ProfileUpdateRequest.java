package oop.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank(message = "姓名不能为空") String name,
        String phone,
        String email) {
}
