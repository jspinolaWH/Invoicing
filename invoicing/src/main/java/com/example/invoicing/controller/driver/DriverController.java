package com.example.invoicing.controller.driver;

import com.example.invoicing.entity.driver.DriverMaster;
import com.example.invoicing.repository.DriverMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverMasterRepository driverMasterRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/search")
    public List<DriverMaster> search(@RequestParam(defaultValue = "") String q) {
        return driverMasterRepository
            .findTop10ByActiveIsTrueAndDriverIdContainingIgnoreCaseOrActiveIsTrueAndNameContainingIgnoreCase(q, q);
    }
}
