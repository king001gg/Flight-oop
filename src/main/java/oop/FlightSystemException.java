package oop;

/**
 * 业务异常：航班/订单操作不满足业务规则时抛出，message 可直接展示给用户。
 */
public class FlightSystemException extends Exception {

    public FlightSystemException(String message) {
        super(message);
    }

    public FlightSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
