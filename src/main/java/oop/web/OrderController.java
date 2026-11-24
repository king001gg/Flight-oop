package oop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import oop.AuthService;
import oop.Customer;
import oop.CustomerStore;
import oop.FlightSystem;
import oop.FlightSystemException;
import oop.Order;
import oop.dto.BookingRequest;
import oop.dto.OrderView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final FlightSystem flightSystem;
    private final CustomerStore customerStore;

    public OrderController(FlightSystem flightSystem, CustomerStore customerStore) {
        this.flightSystem = flightSystem;
        this.customerStore = customerStore;
    }

    @GetMapping
    public ApiResponse<List<OrderView>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        AuthService.Session session = session(request);
        List<Order> orders = session.isAdmin()
                ? flightSystem.getOrders()
                : flightSystem.getOrdersForCustomer(session.customerId());

        String statusFilter = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<OrderView> result = new ArrayList<>();
        for (Order order : orders) {
            if (!statusFilter.isEmpty()
                    && (order.getStatus() == null
                    || !order.getStatus().name().equals(statusFilter))) {
                continue;
            }
            if (!kw.isEmpty() && !matches(order, kw)) {
                continue;
            }
            result.add(OrderView.from(order, flightSystem.findFlight(order.getFlightId())));
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderView> detail(@PathVariable String id, HttpServletRequest request) {
        AuthService.Session session = session(request);
        Order order = flightSystem.findOrder(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在：" + id);
        }
        requireOwnerOrAdmin(order, session);
        return ApiResponse.ok(OrderView.from(order, flightSystem.findFlight(order.getFlightId())));
    }

    @PostMapping
    public ApiResponse<OrderView> book(@Valid @RequestBody BookingRequest body,
                                       HttpServletRequest request)
            throws FlightSystemException {
        AuthService.Session session = session(request);
        String customerId;
        if (session.isAdmin()) {
            customerId = body.customerId();
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new FlightSystemException("管理员订票时必须指定客户证件号");
            }
        } else {
            customerId = session.customerId();
        }
        Customer customer = customerStore.find(customerId);
        if (customer == null) {
            throw new FlightSystemException("客户不存在：" + customerId);
        }
        Order order = flightSystem.book(body.flightId(), customer.getName(),
                customer.getCustomerId(), body.quantity());
        return ApiResponse.ok(OrderView.from(order,
                flightSystem.findFlight(order.getFlightId())));
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<OrderView> refund(@PathVariable String id, HttpServletRequest request)
            throws FlightSystemException {
        AuthService.Session session = session(request);
        Order order = flightSystem.findOrder(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在：" + id);
        }
        requireOwnerOrAdmin(order, session);
        flightSystem.refund(id);
        Order updated = flightSystem.findOrder(id);
        return ApiResponse.ok(OrderView.from(updated,
                flightSystem.findFlight(updated.getFlightId())));
    }

    private static void requireOwnerOrAdmin(Order order, AuthService.Session session) {
        if (!session.isAdmin() && !order.getCustomerId().equals(session.customerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该订单");
        }
    }

    private static boolean matches(Order order, String keyword) {
        return order.getOrderId().toLowerCase(Locale.ROOT).contains(keyword)
                || order.getFlightId().toLowerCase(Locale.ROOT).contains(keyword)
                || order.getCustomerName().toLowerCase(Locale.ROOT).contains(keyword)
                || order.getCustomerId().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static AuthService.Session session(HttpServletRequest request) {
        return (AuthService.Session) request.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);
    }
}
