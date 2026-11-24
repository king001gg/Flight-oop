package oop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 航班订票系统业务核心。
 * <p>负责：【数据持久化】读写 flights.dat / orders.dat；
 * 【航班管理】增删改查；【订票退票】余票校验、订单生成、退票回补；
 * 【统计汇总】航线时段统计与月度售票统计。</p>
 *
 * <p>内部使用的数据结构：</p>
 * <ul>
 *   <li>{@code ArrayList<Flight> flights}：按“日期 → 起飞时间 → 航班号”排序的顺序表，
 *       日期区间查询用二分查找定位边界；</li>
 *   <li>{@code HashMap<String, Flight> flightById}：航班号 → 航班，O(1) 查找；</li>
 *   <li>{@code HashMap<String, List<Flight>> routeIndex}：航线 → 航班列表，支持按城市对查询与满仓推荐；</li>
 *   <li>{@code HashMap<String, Order> orderById}：订单号 → 订单，O(1) 查找。</li>
 * </ul>
 */
public class FlightSystem {

    private static final String FLIGHT_FILE = "flights.dat";
    private static final String ORDER_FILE = "orders.dat";
    private static final DateTimeFormatter ORDER_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Path dataDir;
    private final Path flightFile;
    private final Path orderFile;

    /** 按起飞日期、时间、航班号排序 */
    private final List<Flight> flights = new ArrayList<>();
    private final Map<String, Flight> flightById = new HashMap<>();
    private final Map<String, List<Flight>> routeIndex = new HashMap<>();

    /** 按下单时间倒序排列 */
    private final List<Order> orders = new ArrayList<>();
    private final Map<String, Order> orderById = new HashMap<>();

    public FlightSystem() {
        this(Paths.get("."));
    }

    public FlightSystem(String dataDir) {
        this(Paths.get(dataDir));
    }

    public FlightSystem(Path dataDir) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.flightFile = this.dataDir.resolve(FLIGHT_FILE);
        this.orderFile = this.dataDir.resolve(ORDER_FILE);
    }

    public Path getDataDir() {
        return dataDir;
    }

    // ════════════════════════════════════════════════════════════════
    //  数据加载 / 保存
    // ════════════════════════════════════════════════════════════════

    /**
     * 从数据目录加载航班与订单，并重建索引。
     * 不会抛出异常；解析失败的行会记录到 {@link LoadReport#getWarnings()}。
     */
    public synchronized LoadReport load() {
        flights.clear();
        orders.clear();
        flightById.clear();
        routeIndex.clear();
        orderById.clear();

        List<String> warnings = new ArrayList<>();
        readFlights(warnings);
        readOrders(warnings);
        rebuildIndexes();

        // 丢弃引用了不存在航班的订单
        boolean removed = false;
        Iterator<Order> it = orders.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            if (!flightById.containsKey(order.getFlightId())) {
                warnings.add("订单 " + order.getOrderId() + " 引用了不存在的航班 "
                        + order.getFlightId() + "，已忽略");
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            rebuildIndexes();
        }

        for (Flight flight : flights) {
            if (flight.getBookedSeats() > flight.getTotalSeats()) {
                warnings.add("航班 " + flight.getFlightId() + " 已订座位数（"
                        + flight.getBookedSeats() + "）超过总座位数（"
                        + flight.getTotalSeats() + "）");
            }
        }
        return new LoadReport(flights.size(), orders.size(), warnings);
    }

    private void readFlights(List<String> warnings) {
        if (!Files.exists(flightFile)) {
            warnings.add("未找到 " + FLIGHT_FILE + "，将从空航班数据开始");
            return;
        }
        Set<String> ids = new HashSet<>();
        int lineNo = 0;
        try (BufferedReader reader = Files.newBufferedReader(flightFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    Flight flight = Flight.fromDataLine(line);
                    if (!ids.add(flight.getFlightId())) {
                        warnings.add("flights.dat 第 " + lineNo + " 行：航班编号 "
                                + flight.getFlightId() + " 重复，已忽略");
                        continue;
                    }
                    flights.add(flight);
                } catch (RuntimeException e) {
                    warnings.add("flights.dat 第 " + lineNo + " 行：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            warnings.add("读取 flights.dat 失败：" + e.getMessage());
        }
    }

    private void readOrders(List<String> warnings) {
        if (!Files.exists(orderFile)) {
            warnings.add("未找到 " + ORDER_FILE + "，将从空订单数据开始");
            return;
        }
        Set<String> ids = new HashSet<>();
        int lineNo = 0;
        try (BufferedReader reader = Files.newBufferedReader(orderFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    Order order = Order.fromDataLine(line);
                    if (!ids.add(order.getOrderId())) {
                        warnings.add("orders.dat 第 " + lineNo + " 行：订单编号 "
                                + order.getOrderId() + " 重复，已忽略");
                        continue;
                    }
                    orders.add(order);
                } catch (RuntimeException e) {
                    warnings.add("orders.dat 第 " + lineNo + " 行：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            warnings.add("读取 orders.dat 失败：" + e.getMessage());
        }
    }

    /** 重建所有索引与派生数据（排序、哈希表、航线索引、已订座位） */
    private void rebuildIndexes() {
        Collections.sort(flights);

        flightById.clear();
        routeIndex.clear();
        for (Flight flight : flights) {
            flightById.put(flight.getFlightId(), flight);
            routeIndex.computeIfAbsent(flight.getRoute(), key -> new ArrayList<>()).add(flight);
        }

        orders.sort(Comparator.comparing(Order::getOrderTime).reversed());
        orderById.clear();
        for (Order order : orders) {
            orderById.put(order.getOrderId(), order);
        }

        recomputeBookedSeats();
    }

    /** 已订座位完全由订单推导，保证订单文件是唯一数据源 */
    private void recomputeBookedSeats() {
        for (Flight flight : flights) {
            flight.setBookedSeats(0);
        }
        for (Order order : orders) {
            if (order.getStatus() == Order.Status.BOOKED) {
                Flight flight = flightById.get(order.getFlightId());
                if (flight != null) {
                    flight.setBookedSeats(flight.getBookedSeats() + order.getQuantity());
                }
            }
        }
    }

    private void saveFlights() throws FlightSystemException {
        List<String> lines = new ArrayList<>();
        for (Flight flight : flights) {
            lines.add(flight.toDataLine());
        }
        writeLines(flightFile, lines);
    }

    private void saveOrders() throws FlightSystemException {
        List<String> lines = new ArrayList<>();
        for (Order order : orders) {
            lines.add(order.toDataLine());
        }
        writeLines(orderFile, lines);
    }

    /** 先写临时文件再替换，避免写入中途失败把原文件截断 */
    private void writeLines(Path target, List<String> lines) throws FlightSystemException {
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(dataDir);
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响主流程
            }
            throw new FlightSystemException("保存 " + target.getFileName() + " 失败：" + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  查询
    // ════════════════════════════════════════════════════════════════

    public synchronized List<Flight> getFlights() {
        return new ArrayList<>(flights);
    }

    public synchronized List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public synchronized int getFlightCount() {
        return flights.size();
    }

    public synchronized int getOrderCount() {
        return orders.size();
    }

    public synchronized Flight findFlight(String flightId) {
        return flightById.get(normalize(flightId));
    }

    public synchronized Order findOrder(String orderId) {
        return orderById.get(normalize(orderId));
    }

    /** 按航班号精确查询 */
    public synchronized List<Flight> searchFlightById(String flightId) {
        Flight flight = findFlight(flightId);
        return flight == null ? new ArrayList<>() : new ArrayList<>(List.of(flight));
    }

    /** 按出发城市 + 到达城市查询航线上的全部航班 */
    public synchronized List<Flight> searchFlightsByRoute(String startCity, String endCity) {
        String key = normalize(startCity) + "→" + normalize(endCity);
        return new ArrayList<>(routeIndex.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * 通用航班筛选：城市可留空，日期可空，onlyAvailable 表示只看有票航班。
     */
    public synchronized List<Flight> searchFlights(String startCity, String endCity,
                                                   LocalDate date, boolean onlyAvailable) {
        String start = normalize(startCity);
        String end = normalize(endCity);
        List<Flight> result = new ArrayList<>();
        for (Flight flight : flights) {
            if (!start.isEmpty() && !flight.getStartCity().equals(start)) {
                continue;
            }
            if (!end.isEmpty() && !flight.getEndCity().equals(end)) {
                continue;
            }
            if (date != null && !flight.getFlightDate().equals(date)) {
                continue;
            }
            if (onlyAvailable && flight.isFull()) {
                continue;
            }
            result.add(flight);
        }
        return result;
    }

    /** 同航线、余票不少于 quantity 的替代航班（按起飞时间排序） */
    public synchronized List<Flight> findAlternatives(Flight flight, int quantity) {
        int need = Math.max(1, quantity);
        List<Flight> result = new ArrayList<>();
        for (Flight candidate : routeIndex.getOrDefault(flight.getRoute(), Collections.emptyList())) {
            if (!candidate.getFlightId().equals(flight.getFlightId())
                    && candidate.getRemainingSeats() >= need) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** 按订单号 / 姓名 / 证件号 / 航班号模糊查询订单 */
    public synchronized List<Order> searchOrders(String keyword) {
        String text = normalize(keyword);
        if (text.isEmpty()) {
            return new ArrayList<>(orders);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<Order> result = new ArrayList<>();
        for (Order order : orders) {
            if (order.getOrderId().toLowerCase(Locale.ROOT).contains(lower)
                    || order.getFlightId().toLowerCase(Locale.ROOT).contains(lower)
                    || order.getCustomerName().contains(text)
                    || order.getCustomerId().toLowerCase(Locale.ROOT).contains(lower)) {
                result.add(order);
            }
        }
        return result;
    }

    public synchronized List<Order> getOrdersForFlight(String flightId) {
        String id = normalize(flightId);
        List<Order> result = new ArrayList<>();
        for (Order order : orders) {
            if (order.getFlightId().equals(id)) {
                result.add(order);
            }
        }
        return result;
    }

    /** 某个客户（按证件号）的全部订单，用于客户订单列表与退票权限校验 */
    public synchronized List<Order> getOrdersForCustomer(String customerId) {
        String id = normalize(customerId);
        List<Order> result = new ArrayList<>();
        if (id.isEmpty()) {
            return result;
        }
        for (Order order : orders) {
            if (order.getCustomerId().equals(id)) {
                result.add(order);
            }
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  航班管理
    // ════════════════════════════════════════════════════════════════

    public synchronized void addFlight(Flight flight) throws FlightSystemException {
        validateFlight(flight);
        if (flightById.containsKey(flight.getFlightId())) {
            throw new FlightSystemException("航班编号「" + flight.getFlightId() + "」已存在");
        }
        flight.setBookedSeats(0);
        flights.add(flight);
        rebuildIndexes();
        try {
            saveFlights();
        } catch (FlightSystemException e) {
            flights.remove(flight);
            rebuildIndexes();
            throw e;
        }
    }

    public synchronized void updateFlight(String oldFlightId, Flight updated) throws FlightSystemException {
        Flight existing = flightById.get(normalize(oldFlightId));
        if (existing == null) {
            throw new FlightSystemException("航班不存在：" + oldFlightId);
        }
        validateFlight(updated);

        boolean idChanged = !existing.getFlightId().equals(updated.getFlightId());
        if (idChanged && flightById.containsKey(updated.getFlightId())) {
            throw new FlightSystemException("航班编号「" + updated.getFlightId() + "」已存在");
        }
        if (idChanged && !getOrdersForFlight(existing.getFlightId()).isEmpty()) {
            throw new FlightSystemException("该航班已有订单记录，不能修改航班编号");
        }
        if (updated.getTotalSeats() < existing.getBookedSeats()) {
            throw new FlightSystemException("总座位数不能小于已订座位数（"
                    + existing.getBookedSeats() + "）");
        }

        updated.setBookedSeats(existing.getBookedSeats());
        flights.remove(existing);
        flights.add(updated);
        rebuildIndexes();
        try {
            saveFlights();
        } catch (FlightSystemException e) {
            flights.remove(updated);
            flights.add(existing);
            rebuildIndexes();
            throw e;
        }
    }

    /**
     * 按航线批量设置折扣，只修改航班价格字段并一次性保存。
     *
     * @return 受影响的航班数
     */
    public synchronized int applyRouteDiscount(String startCity, String endCity, double discount)
            throws FlightSystemException {
        if (isBlank(startCity) || isBlank(endCity)) {
            throw new FlightSystemException("出发城市和到达城市不能为空");
        }
        if (!Double.isFinite(discount) || discount <= 0 || discount > 1) {
            throw new FlightSystemException("折扣必须在 0~1 之间（如 0.85 表示 8.5 折）");
        }
        String start = startCity.trim();
        String end = endCity.trim();
        List<Flight> matched = new ArrayList<>();
        for (Flight flight : flights) {
            if (flight.getStartCity().equals(start) && flight.getEndCity().equals(end)) {
                matched.add(flight);
            }
        }
        if (matched.isEmpty()) {
            throw new FlightSystemException("该航线没有航班：" + start + " → " + end);
        }
        List<Double> oldDiscounts = new ArrayList<>();
        for (Flight flight : matched) {
            oldDiscounts.add(flight.getDiscount());
            flight.setDiscount(discount);
        }
        try {
            saveFlights();
        } catch (FlightSystemException e) {
            for (int i = 0; i < matched.size(); i++) {
                matched.get(i).setDiscount(oldDiscounts.get(i));
            }
            throw e;
        }
        return matched.size();
    }

    public synchronized void deleteFlight(String flightId) throws FlightSystemException {
        Flight flight = flightById.get(normalize(flightId));
        if (flight == null) {
            throw new FlightSystemException("航班不存在：" + flightId);
        }
        if (!getOrdersForFlight(flight.getFlightId()).isEmpty()) {
            throw new FlightSystemException("该航班已有订单记录，不能删除");
        }
        flights.remove(flight);
        rebuildIndexes();
        try {
            saveFlights();
        } catch (FlightSystemException e) {
            flights.add(flight);
            rebuildIndexes();
            throw e;
        }
    }

    private void validateFlight(Flight flight) throws FlightSystemException {
        if (flight == null) {
            throw new FlightSystemException("航班信息不能为空");
        }
        if (isBlank(flight.getFlightId())) {
            throw new FlightSystemException("航班编号不能为空");
        }
        if (flight.getFlightDate() == null || flight.getTakeOffTime() == null) {
            throw new FlightSystemException("航班日期和起飞时间不能为空");
        }
        if (flight.getDurationMinutes() <= 0) {
            throw new FlightSystemException("飞行时长必须大于 0");
        }
        if (!Double.isFinite(flight.getPrice()) || flight.getPrice() < 0) {
            throw new FlightSystemException("票价必须是非负数字");
        }
        if (!Double.isFinite(flight.getDiscount()) || flight.getDiscount() <= 0
                || flight.getDiscount() > 1) {
            throw new FlightSystemException("折扣必须在 0~1 之间（如 0.85 表示 8.5 折）");
        }
        if (flight.getTotalSeats() <= 0) {
            throw new FlightSystemException("总座位数必须大于 0");
        }
        if (!Double.isFinite(flight.getOnTimeRate()) || flight.getOnTimeRate() < 0
                || flight.getOnTimeRate() > 100) {
            throw new FlightSystemException("准点率必须在 0~100 之间");
        }
        checkNoDelimiter(flight.getFlightId(), "航班编号");
        checkNoDelimiter(flight.getStartCity(), "出发城市");
        checkNoDelimiter(flight.getEndCity(), "到达城市");
        checkNoDelimiter(flight.getPlaneModel(), "飞机型号");
        checkNoDelimiter(flight.getAirline(), "航空公司");
    }

    // ════════════════════════════════════════════════════════════════
    //  订票 / 退票
    // ════════════════════════════════════════════════════════════════

    /**
     * 订票。余票不足时抛出 {@link NoSeatsException}，异常中携带同航线的替代航班。
     */
    public synchronized Order book(String flightId, String customerName, String customerId, int quantity)
            throws FlightSystemException {
        Flight flight = flightById.get(normalize(flightId));
        if (flight == null) {
            throw new FlightSystemException("航班不存在：" + flightId);
        }
        if (isBlank(customerName)) {
            throw new FlightSystemException("客户姓名不能为空");
        }
        if (isBlank(customerId)) {
            throw new FlightSystemException("证件号不能为空");
        }
        if (quantity <= 0) {
            throw new FlightSystemException("订票数量必须大于 0");
        }
        checkNoDelimiter(customerName, "客户姓名");
        checkNoDelimiter(customerId, "证件号");

        if (flight.getRemainingSeats() < quantity) {
            throw new NoSeatsException(
                    "航班 " + flight.getFlightId() + " 余票不足：剩余 "
                            + flight.getRemainingSeats() + " 张，本次需要 " + quantity + " 张。",
                    findAlternatives(flight, quantity));
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(generateOrderId(now), flight.getFlightId(),
                customerName.trim(), customerId.trim(), quantity, now,
                flight.getActualPrice(), Order.Status.BOOKED, null);
        orders.add(order);
        rebuildIndexes();
        try {
            saveOrders();
        } catch (FlightSystemException e) {
            orders.remove(order);
            rebuildIndexes();
            throw e;
        }
        return order;
    }

    /** 退票：整张订单退票，座位自动回补 */
    public synchronized void refund(String orderId) throws FlightSystemException {
        Order order = orderById.get(normalize(orderId));
        if (order == null) {
            throw new FlightSystemException("订单不存在：" + orderId);
        }
        if (order.isRefunded()) {
            throw new FlightSystemException("订单 " + order.getOrderId() + " 已退票，不能重复退票");
        }

        Order.Status oldStatus = order.getStatus();
        LocalDateTime oldRefundTime = order.getRefundTime();
        order.setStatus(Order.Status.REFUNDED);
        order.setRefundTime(LocalDateTime.now());
        rebuildIndexes();
        try {
            saveOrders();
        } catch (FlightSystemException e) {
            order.setStatus(oldStatus);
            order.setRefundTime(oldRefundTime);
            rebuildIndexes();
            throw e;
        }
    }

    private String generateOrderId(LocalDateTime time) {
        String base = "ORD" + time.format(ORDER_ID_TIME);
        int sequence = 1;
        String id;
        do {
            id = base + "-" + String.format("%03d", sequence++);
        } while (orderById.containsKey(id));
        return id;
    }

    // ════════════════════════════════════════════════════════════════
    //  统计汇总
    // ════════════════════════════════════════════════════════════════

    /**
     * 统计某航线（城市留空表示全部航线）在 [from, to] 内的飞行次数、座位数与载客人数。
     * 日期区间先用二分查找定位有序航班表的边界，再逐条筛选航线。
     */
    public synchronized RouteStatistics statisticsForRoute(String startCity, String endCity,
                                              LocalDate from, LocalDate to)
            throws FlightSystemException {
        if (from == null || to == null) {
            throw new FlightSystemException("请填写开始日期和结束日期");
        }
        if (from.isAfter(to)) {
            throw new FlightSystemException("开始日期不能晚于结束日期");
        }
        String start = normalize(startCity);
        String end = normalize(endCity);
        boolean allRoutes = start.isEmpty() && end.isEmpty();
        if (!allRoutes && (start.isEmpty() || end.isEmpty())) {
            throw new FlightSystemException("请同时填写出发城市和到达城市，或都留空统计全部航线");
        }

        int begin = firstFlightIndexOnOrAfter(from);
        int finish = firstFlightIndexAfter(to);
        int flightCount = 0;
        int totalSeats = 0;
        int passengers = 0;
        for (int i = begin; i < finish; i++) {
            Flight flight = flights.get(i);
            if (!allRoutes && (!flight.getStartCity().equals(start)
                    || !flight.getEndCity().equals(end))) {
                continue;
            }
            flightCount++;
            totalSeats += flight.getTotalSeats();
            passengers += flight.getBookedSeats();
        }
        return new RouteStatistics(flightCount, totalSeats, passengers);
    }

    private int firstFlightIndexOnOrAfter(LocalDate date) {
        int low = 0;
        int high = flights.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (flights.get(mid).getFlightDate().isBefore(date)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private int firstFlightIndexAfter(LocalDate date) {
        int low = 0;
        int high = flights.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (!flights.get(mid).getFlightDate().isAfter(date)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * 月度售票统计：售票按下单月份归档，退票按退票月份归档；
     * 净销售额 = 当月售票金额 − 当月退票金额。
     */
    public synchronized List<MonthStatistics> monthlySales() {
        Map<YearMonth, Integer> sold = new TreeMap<>();
        Map<YearMonth, Integer> refunded = new TreeMap<>();
        Map<YearMonth, Double> revenue = new TreeMap<>();

        for (Order order : orders) {
            YearMonth orderMonth = YearMonth.from(order.getOrderTime());
            sold.merge(orderMonth, order.getQuantity(), Integer::sum);
            revenue.merge(orderMonth, order.getAmount(), Double::sum);

            if (order.getStatus() == Order.Status.REFUNDED && order.getRefundTime() != null) {
                YearMonth refundMonth = YearMonth.from(order.getRefundTime());
                refunded.merge(refundMonth, order.getQuantity(), Integer::sum);
                revenue.merge(refundMonth, -order.getAmount(), Double::sum);
            }
        }

        TreeSet<YearMonth> months = new TreeSet<>();
        months.addAll(sold.keySet());
        months.addAll(refunded.keySet());

        List<MonthStatistics> result = new ArrayList<>();
        for (YearMonth month : months) {
            result.add(new MonthStatistics(month,
                    sold.getOrDefault(month, 0),
                    refunded.getOrDefault(month, 0),
                    Flight.round2(revenue.getOrDefault(month, 0.0))));
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  工具方法
    // ════════════════════════════════════════════════════════════════

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static void checkNoDelimiter(String value, String fieldName)
            throws FlightSystemException {
        if (value != null && value.contains("::")) {
            throw new FlightSystemException(fieldName + "不能包含分隔符「::」");
        }
    }

    /** 加载结果：有效记录数与告警信息 */
    public static class LoadReport {
        private final int flightCount;
        private final int orderCount;
        private final List<String> warnings;

        public LoadReport(int flightCount, int orderCount, List<String> warnings) {
            this.flightCount = flightCount;
            this.orderCount = orderCount;
            this.warnings = warnings;
        }

        public synchronized int getFlightCount() {
            return flightCount;
        }

        public synchronized int getOrderCount() {
            return orderCount;
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }
}
