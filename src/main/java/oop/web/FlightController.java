package oop.web;

import oop.Flight;
import oop.FlightSystem;
import oop.dto.FlightView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightSystem flightSystem;

    public FlightController(FlightSystem flightSystem) {
        this.flightSystem = flightSystem;
    }

    @GetMapping
    public ApiResponse<List<FlightView>> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        List<FlightView> result = flightSystem.searchFlights(from, to, date, onlyAvailable)
                .stream()
                .map(FlightView::from)
                .toList();
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<FlightView> detail(@PathVariable String id) {
        Flight flight = flightSystem.findFlight(id);
        if (flight == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "航班不存在：" + id);
        }
        return ApiResponse.ok(FlightView.from(flight));
    }
}
