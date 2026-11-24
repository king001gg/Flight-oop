package oop;

import java.util.List;

/**
 * 余票不足异常。
 * <p>除了提示信息外，还携带同航线的可替代航班列表，供界面推荐给用户。</p>
 */
public class NoSeatsException extends FlightSystemException {

    private final List<Flight> alternatives;

    public NoSeatsException(String message, List<Flight> alternatives) {
        super(message);
        this.alternatives = alternatives;
    }

    /** 同航线、余票足够、按起飞时间排序的可选航班 */
    public List<Flight> getAlternatives() {
        return alternatives;
    }
}
