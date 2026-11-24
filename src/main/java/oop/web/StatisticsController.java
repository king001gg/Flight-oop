package oop.web;

import jakarta.servlet.http.HttpServletRequest;
import oop.AuthService;
import oop.Flight;
import oop.FlightSystem;
import oop.FlightSystemException;
import oop.Order;
import oop.dto.MonthlyStatsView;
import oop.dto.RouteStatsView;
import oop.dto.SummaryView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatisticsController {

    private final FlightSystem flightSystem;

    public StatisticsController(FlightSystem flightSystem) {
        this.flightSystem = flightSystem;
    }

    @GetMapping("/route")
    public ApiResponse<RouteStatsView> route(
            @RequestParam(defaultValue = "") String startCity,
            @RequestParam(defaultValue = "") String endCity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) throws FlightSystemException {
        requireAdmin(request);
        return ApiResponse.ok(RouteStatsView.from(
                flightSystem.statisticsForRoute(startCity, endCity, from, to)));
    }

    @GetMapping("/monthly")
    public ApiResponse<List<MonthlyStatsView>> monthly(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(flightSystem.monthlySales().stream()
                .map(MonthlyStatsView::from)
                .toList());
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryView> summary() {
        List<Order> orders = flightSystem.getOrders();
        int activeOrderCount = 0;
        int passengerCount = 0;
        double revenue = 0;
        for (Order order : orders) {
            if (order.getStatus() == Order.Status.BOOKED) {
                activeOrderCount++;
                passengerCount += order.getQuantity();
                revenue += order.getAmount();
            }
        }
        return ApiResponse.ok(new SummaryView(
                flightSystem.getFlightCount(),
                orders.size(),
                activeOrderCount,
                passengerCount,
                Flight.round2(revenue)));
    }

    private static void requireAdmin(HttpServletRequest request) {
        AuthService.Session session = (AuthService.Session)
                request.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);
        if (session == null || !session.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问统计报表");
        }
    }
}
