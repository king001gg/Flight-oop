package oop.web;

import jakarta.validation.Valid;
import oop.Flight;
import oop.FlightSystem;
import oop.FlightSystemException;
import oop.dto.BulkFareRequest;
import oop.dto.BulkFareResult;
import oop.dto.FareRequest;
import oop.dto.FlightView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminFareController {

    private final FlightSystem flightSystem;

    public AdminFareController(FlightSystem flightSystem) {
        this.flightSystem = flightSystem;
    }

    @PutMapping("/flights/{id}/fare")
    public ApiResponse<FlightView> updateFare(@PathVariable String id,
                                              @Valid @RequestBody FareRequest request)
            throws FlightSystemException {
        Flight existing = flightSystem.findFlight(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "航班不存在：" + id);
        }
        Flight updated = existing.copy();
        updated.setPrice(request.price());
        updated.setDiscount(request.discount());
        flightSystem.updateFlight(id, updated);
        return ApiResponse.ok(FlightView.from(flightSystem.findFlight(id)));
    }

    @PostMapping("/fares/bulk")
    public ApiResponse<BulkFareResult> bulkDiscount(@Valid @RequestBody BulkFareRequest request)
            throws FlightSystemException {
        int updated = flightSystem.applyRouteDiscount(
                request.startCity(), request.endCity(), request.discount());
        return ApiResponse.ok(new BulkFareResult(updated));
    }
}
