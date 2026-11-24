package oop.web;

import jakarta.validation.Valid;
import oop.Customer;
import oop.CustomerStore;
import oop.FlightSystem;
import oop.FlightSystemException;
import oop.Order;
import oop.dto.CustomerRequest;
import oop.dto.CustomerView;
import oop.dto.OrderView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerStore customerStore;
    private final FlightSystem flightSystem;

    public AdminCustomerController(CustomerStore customerStore, FlightSystem flightSystem) {
        this.customerStore = customerStore;
        this.flightSystem = flightSystem;
    }

    @GetMapping
    public ApiResponse<List<CustomerView>> list(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerStore.search(keyword).stream()
                .map(CustomerView::from)
                .toList());
    }

    @PostMapping
    public ApiResponse<CustomerView> create(@Valid @RequestBody CustomerRequest body)
            throws FlightSystemException {
        Customer customer = toCustomer(body, LocalDateTime.now());
        customerStore.add(customer);
        return ApiResponse.ok(CustomerView.from(customerStore.find(customer.getCustomerId())));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerView> update(@PathVariable String id,
                                            @Valid @RequestBody CustomerRequest body)
            throws FlightSystemException {
        Customer existing = customerStore.find(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在：" + id);
        }
        if (!existing.getCustomerId().equals(body.customerId().trim())) {
            throw new FlightSystemException("证件号是客户主键，不能修改");
        }
        Customer updated = toCustomer(body, existing.getCreatedAt());
        customerStore.update(id, updated);
        return ApiResponse.ok(CustomerView.from(customerStore.find(id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) throws FlightSystemException {
        if (customerStore.find(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在：" + id);
        }
        if (!flightSystem.getOrdersForCustomer(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "该客户存在订单记录，不能删除");
        }
        customerStore.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/orders")
    public ApiResponse<List<OrderView>> orders(@PathVariable String id) {
        if (customerStore.find(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在：" + id);
        }
        List<OrderView> result = flightSystem.getOrdersForCustomer(id).stream()
                .map(order -> OrderView.from(order,
                        flightSystem.findFlight(order.getFlightId())))
                .toList();
        return ApiResponse.ok(result);
    }

    private static Customer toCustomer(CustomerRequest body, LocalDateTime createdAt)
            throws FlightSystemException {
        try {
            return body.toCustomer(createdAt);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
    }
}
