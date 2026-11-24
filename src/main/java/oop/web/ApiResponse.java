package oop.web;

/**
 * 统一 API 响应：code=0 表示成功。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
