package com.example.invoicing.controller.location;

import com.example.invoicing.entity.location.LocationMaster;
import com.example.invoicing.entity.location.dto.MunicipalityResult;
import com.example.invoicing.repository.LocationMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationMasterRepository locationMasterRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/api/v1/locations/search")
    public List<LocationMaster> searchLocations(@RequestParam(defaultValue = "") String q) {
        return locationMasterRepository.searchLocations(q);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/api/v1/municipalities/search")
    public List<MunicipalityResult> searchMunicipalities(@RequestParam(defaultValue = "") String q) {
        return locationMasterRepository.searchMunicipalities(q);
    }
}
