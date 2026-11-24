package oop.web;

import jakarta.validation.Valid;
import oop.Flight;
import oop.FlightSystem;
import oop.FlightSystemException;
import oop.dto.FlightRequest;
import oop.dto.FlightView;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/flights")
public class AdminFlightController {

    private final FlightSystem flightSystem;

    public AdminFlightController(FlightSystem flightSystem) {
        this.flightSystem = flightSystem;
    }

    @GetMapping
    public ApiResponse<List<FlightView>> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<FlightView> result = flightSystem.searchFlights(from, to, date, false)
                .stream()
                .map(FlightView::from)
                .toList();
        return ApiResponse.ok(result);
    }

    @PostMapping
    public ApiResponse<FlightView> create(@Valid @RequestBody FlightRequest request)
            throws FlightSystemException {
        Flight flight = request.toFlight();
        flightSystem.addFlight(flight);
        return ApiResponse.ok(FlightView.from(flightSystem.findFlight(flight.getFlightId())));
    }

    @PutMapping("/{id}")
    public ApiResponse<FlightView> update(@PathVariable String id,
                                          @Valid @RequestBody FlightRequest request)
            throws FlightSystemException {
        if (flightSystem.findFlight(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "航班不存在：" + id);
        }
        Flight flight = request.toFlight();
        flightSystem.updateFlight(id, flight);
        return ApiResponse.ok(FlightView.from(flightSystem.findFlight(flight.getFlightId())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) throws FlightSystemException {
        flightSystem.deleteFlight(id);
        return ApiResponse.ok(null);
    }
}
