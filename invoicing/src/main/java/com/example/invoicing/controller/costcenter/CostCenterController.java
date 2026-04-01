package com.example.invoicing.controller.costcenter;

import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.costcenter.dto.CostCenterRequest;
import com.example.invoicing.entity.costcenter.dto.CostCenterResponse;
import com.example.invoicing.service.CostCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService service;

    @GetMapping
    public List<CostCenterResponse> getAll(
            @RequestParam(required = false) String productSegment,
            @RequestParam(required = false) String responsibilitySegment) {

        List<CostCenter> results;
        if (productSegment != null) {
            results = service.findByProductSegment(productSegment.toUpperCase());
        } else if (responsibilitySegment != null) {
            results = service.findByResponsibilitySegment(responsibilitySegment.toUpperCase());
        } else {
            results = service.findAll();
        }
        return results.stream().map(CostCenterResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CostCenterResponse getById(@PathVariable Long id) {
        return CostCenterResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CostCenterResponse create(@RequestBody CostCenterRequest request) {
        return CostCenterResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public CostCenterResponse update(@PathVariable Long id, @RequestBody CostCenterRequest request) {
        return CostCenterResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
