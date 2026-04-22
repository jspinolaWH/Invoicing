package com.example.invoicing.controller.pricelist;

import com.example.invoicing.entity.pricelist.dto.PriceListRequest;
import com.example.invoicing.entity.pricelist.dto.PriceListResponse;
import com.example.invoicing.service.PriceListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/price-lists")
@RequiredArgsConstructor
public class PriceListController {

    private final PriceListService service;

    @GetMapping
    public List<PriceListResponse> getAll(@RequestParam(required = false) Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            return service.findActive().stream().map(PriceListResponse::from).toList();
        }
        return service.findAll().stream().map(PriceListResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PriceListResponse getById(@PathVariable Long id) {
        return PriceListResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceListResponse create(@RequestBody PriceListRequest request) {
        return PriceListResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public PriceListResponse update(@PathVariable Long id, @RequestBody PriceListRequest request) {
        return PriceListResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
