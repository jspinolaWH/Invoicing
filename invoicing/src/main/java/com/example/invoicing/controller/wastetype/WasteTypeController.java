package com.example.invoicing.controller.wastetype;

import com.example.invoicing.entity.wastetype.WasteTypeMaster;
import com.example.invoicing.repository.WasteTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waste-types")
@RequiredArgsConstructor
public class WasteTypeController {

    private final WasteTypeMasterRepository wasteTypeMasterRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/search")
    public List<WasteTypeMaster> search(@RequestParam(defaultValue = "") String q) {
        return wasteTypeMasterRepository.searchWasteTypes(q);
    }
}
