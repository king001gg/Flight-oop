package oop.web;

import oop.FlightSystemException;
import oop.NoSeatsException;
import oop.dto.FlightView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NoSeatsException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNoSeats(NoSeatsException e) {
        List<FlightView> alternatives = e.getAlternatives() == null
                ? List.of()
                : e.getAlternatives().stream().map(FlightView::from).toList();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, e.getMessage(),
                        Map.of("alternatives", alternatives)));
    }

    @ExceptionHandler(FlightSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(FlightSystemException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "请求体格式错误", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数格式错误：" + e.getName(), null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "缺少参数：" + e.getParameterName(), null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        String message = e.getReason() == null ? "请求失败" : e.getReason();
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error(status, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误", null));
    }
}
