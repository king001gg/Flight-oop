package oop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import oop.AuthService;
import oop.Customer;
import oop.CustomerStore;
import oop.FlightSystemException;
import oop.dto.CustomerView;
import oop.dto.ProfileUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerStore customerStore;

    public CustomerController(CustomerStore customerStore) {
        this.customerStore = customerStore;
    }

    @GetMapping("/me")
    public ApiResponse<CustomerView> me(HttpServletRequest request) {
        AuthService.Session session = session(request);
        if (session.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员账号没有客户资料");
        }
        Customer customer = customerStore.find(session.customerId());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户资料不存在");
        }
        return ApiResponse.ok(CustomerView.from(customer));
    }

    @PutMapping("/me")
    public ApiResponse<CustomerView> update(@Valid @RequestBody ProfileUpdateRequest body,
                                            HttpServletRequest request)
            throws FlightSystemException {
        AuthService.Session session = session(request);
        if (session.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员账号没有客户资料");
        }
        Customer existing = customerStore.find(session.customerId());
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户资料不存在");
        }
        Customer updated = new Customer(
                existing.getCustomerId(),
                body.name().trim(),
                body.phone() == null ? "" : body.phone().trim(),
                body.email() == null ? "" : body.email().trim(),
                existing.getLevel(),
                existing.getCreatedAt());
        customerStore.update(existing.getCustomerId(), updated);
        return ApiResponse.ok(CustomerView.from(customerStore.find(existing.getCustomerId())));
    }

    private static AuthService.Session session(HttpServletRequest request) {
        return (AuthService.Session) request.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);
    }
}
