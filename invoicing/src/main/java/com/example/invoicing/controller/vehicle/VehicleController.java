package com.example.invoicing.controller.vehicle;

import com.example.invoicing.entity.vehicle.Vehicle;
import com.example.invoicing.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleRepository vehicleRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/search")
    public List<Vehicle> search(@RequestParam(defaultValue = "") String q) {
        return vehicleRepository
            .findTop10ByActiveIsTrueAndVehicleIdContainingIgnoreCaseOrActiveIsTrueAndRegistrationPlateContainingIgnoreCase(q, q);
    }
}
