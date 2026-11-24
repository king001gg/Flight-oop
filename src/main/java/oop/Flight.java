package oop;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 航班实体类。
 * <p>一条 Flight 记录代表“某一天执飞的一个航班”，包含航线、时刻、票价、
 * 折扣、座位容量等订票所需信息。</p>
 */
public class Flight implements Comparable<Flight> {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private String flightId;          // 航班编号
    private LocalDate flightDate;     // 执飞日期
    private LocalTime takeOffTime;    // 起飞时间
    private int durationMinutes;      // 飞行时长（分钟）
    private String startCity;         // 出发城市
    private String endCity;           // 到达城市
    private String planeModel;        // 飞机型号
    private double price;             // 原价
    private double discount;          // 折扣（0.85 表示 8.5 折）
    private int totalSeats;           // 总座位数
    private String airline;           // 所属航空公司
    private double onTimeRate;        // 准点率（%）

    /** 已订座位数：由订单实时汇总得到，不写入数据文件 */
    private int bookedSeats;

    public Flight() {
    }

    public Flight(String flightId, LocalDate flightDate, LocalTime takeOffTime, int durationMinutes,
                  String startCity, String endCity, String planeModel,
                  double price, double discount, int totalSeats,
                  String airline, double onTimeRate) {
        this.flightId = flightId;
        this.flightDate = flightDate;
        this.takeOffTime = takeOffTime;
        this.durationMinutes = durationMinutes;
        this.startCity = startCity;
        this.endCity = endCity;
        this.planeModel = planeModel;
        this.price = price;
        this.discount = discount;
        this.totalSeats = totalSeats;
        this.airline = airline;
        this.onTimeRate = onTimeRate;
    }

    // ── getter / setter ─────────────────────────────────────────

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public LocalDate getFlightDate() {
        return flightDate;
    }

    public void setFlightDate(LocalDate flightDate) {
        this.flightDate = flightDate;
    }

    public LocalTime getTakeOffTime() {
        return takeOffTime;
    }

    public void setTakeOffTime(LocalTime takeOffTime) {
        this.takeOffTime = takeOffTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getStartCity() {
        return startCity;
    }

    public void setStartCity(String startCity) {
        this.startCity = startCity;
    }

    public String getEndCity() {
        return endCity;
    }

    public void setEndCity(String endCity) {
        this.endCity = endCity;
    }

    public String getPlaneModel() {
        return planeModel;
    }

    public void setPlaneModel(String planeModel) {
        this.planeModel = planeModel;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public double getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(double onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public int getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(int bookedSeats) {
        this.bookedSeats = bookedSeats;
    }

    // ── 派生属性 ────────────────────────────────────────────────

    public LocalDateTime getTakeOffDateTime() {
        return LocalDateTime.of(flightDate, takeOffTime);
    }

    public LocalDateTime getArrivalDateTime() {
        return getTakeOffDateTime().plusMinutes(durationMinutes);
    }

    /** 到达时刻文本，跨天时追加“（次日）” */
    public String getArrivalTimeText() {
        LocalDateTime arrival = getArrivalDateTime();
        String text = arrival.toLocalTime().format(TIME_FORMAT);
        if (!arrival.toLocalDate().equals(flightDate)) {
            text += "（次日）";
        }
        return text;
    }

    public String getDateText() {
        return flightDate == null ? "" : flightDate.format(DATE_FORMAT);
    }

    public String getTimeText() {
        return takeOffTime == null ? "" : takeOffTime.format(TIME_FORMAT);
    }

    public String getRoute() {
        return startCity + "→" + endCity;
    }

    /** 复制一个航班对象（不复制运行期汇总的已订座位） */
    public Flight copy() {
        Flight copy = new Flight(flightId, flightDate, takeOffTime, durationMinutes,
                startCity, endCity, planeModel, price, discount, totalSeats,
                airline, onTimeRate);
        copy.setBookedSeats(bookedSeats);
        return copy;
    }

    /** 折后实付票价 */
    public double getActualPrice() {
        return round2(price * discount);
    }

    /** 余票数（不会小于 0） */
    public int getRemainingSeats() {
        return Math.max(0, totalSeats - bookedSeats);
    }

    /** 是否满仓 */
    public boolean isFull() {
        return getRemainingSeats() <= 0;
    }

    @Override
    public int compareTo(Flight other) {
        int cmp = flightDate.compareTo(other.flightDate);
        if (cmp != 0) {
            return cmp;
        }
        cmp = takeOffTime.compareTo(other.takeOffTime);
        if (cmp != 0) {
            return cmp;
        }
        return flightId.compareTo(other.flightId);
    }

    // ── 文件序列化 ──────────────────────────────────────────────

    /** 序列化为 flights.dat 中的一行（12 个字段，:: 分隔） */
    public String toDataLine() {
        return String.join("::",
                flightId,
                getDateText(),
                getTimeText(),
                String.valueOf(durationMinutes),
                startCity,
                endCity,
                planeModel,
                String.valueOf(price),
                String.valueOf(discount),
                String.valueOf(totalSeats),
                airline,
                String.valueOf(onTimeRate));
    }

    /** 从 flights.dat 的一行解析航班 */
    public static Flight fromDataLine(String line) {
        String[] parts = line.split("::", -1);
        if (parts.length != 12) {
            throw new IllegalArgumentException("字段数应为 12，实际为 " + parts.length);
        }
        Flight f = new Flight();
        f.setFlightId(requireText(parts[0], "航班编号"));
        f.setFlightDate(parseDate(parts[1]));
        f.setTakeOffTime(parseTime(parts[2]));
        f.setDurationMinutes(parseInt(parts[3], "飞行时长"));
        f.setStartCity(requireText(parts[4], "出发城市"));
        f.setEndCity(requireText(parts[5], "到达城市"));
        f.setPlaneModel(requireText(parts[6], "飞机型号"));
        f.setPrice(parseDouble(parts[7], "票价"));
        f.setDiscount(parseDouble(parts[8], "折扣"));
        f.setTotalSeats(parseInt(parts[9], "总座位数"));
        f.setAirline(requireText(parts[10], "航空公司"));
        f.setOnTimeRate(parseDouble(parts[11], "准点率"));
        validate(f);
        return f;
    }

    public static void validate(Flight f) {
        if (f.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("飞行时长必须大于 0");
        }
        if (f.getPrice() < 0) {
            throw new IllegalArgumentException("票价不能为负数");
        }
        if (f.getDiscount() <= 0 || f.getDiscount() > 1) {
            throw new IllegalArgumentException("折扣必须在 0~1 之间（如 0.85 表示 8.5 折）");
        }
        if (f.getTotalSeats() <= 0) {
            throw new IllegalArgumentException("总座位数必须大于 0");
        }
        if (f.getOnTimeRate() < 0 || f.getOnTimeRate() > 100) {
            throw new IllegalArgumentException("准点率必须在 0~100 之间");
        }
    }

    // ── 解析辅助 ────────────────────────────────────────────────

    private static String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return text.trim();
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd：" + text);
        }
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("起飞时间格式应为 HH:mm：" + text);
        }
    }

    private static int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是整数：" + text);
        }
    }

    private static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是数字：" + text);
        }
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
