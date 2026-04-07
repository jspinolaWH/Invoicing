package com.example.invoicing.controller.seasonalfee;
import com.example.invoicing.entity.seasonalfee.SeasonalFeeConfig;
import com.example.invoicing.service.SeasonalFeeGenerationService;
import com.example.invoicing.repository.SeasonalFeeConfigRepository;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.repository.ProductRepository;
import com.example.invoicing.entity.seasonalfee.dto.GenerateNowResponse;
import com.example.invoicing.entity.seasonalfee.dto.SeasonalFeeConfigRequest;
import com.example.invoicing.entity.seasonalfee.dto.SeasonalFeeConfigResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/seasonal-fees")
@RequiredArgsConstructor
public class SeasonalFeeConfigController {

    private final SeasonalFeeConfigRepository repository;
    private final SeasonalFeeGenerationService generationService;
    private final ProductRepository productRepository;

    @GetMapping
    public List<SeasonalFeeConfigResponse> list(@RequestParam(required = false) String customerNumber) {
        List<SeasonalFeeConfig> configs = (customerNumber != null && !customerNumber.isBlank())
            ? repository.findByCustomerNumberOrderByNextDueDateAsc(customerNumber)
            : repository.findAllByActiveTrueOrderByNextDueDateAsc();
        return configs.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SeasonalFeeConfigResponse get(@PathVariable Long id) {
        return toResponse(load(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeasonalFeeConfigResponse create(@Valid @RequestBody SeasonalFeeConfigRequest req) {
        Product product = productRepository.findById(req.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.getProductId()));
        SeasonalFeeConfig config = SeasonalFeeConfig.builder()
            .customerNumber(req.getCustomerNumber())
            .product(product)
            .billingFrequency(req.getBillingFrequency())
            .amount(req.getAmount())
            .nextDueDate(req.getNextDueDate())
            .propertyReference(req.getPropertyReference())
            .description(req.getDescription())
            .active(req.isActive())
            .build();
        return toResponse(repository.save(config));
    }

    @PutMapping("/{id}")
    public SeasonalFeeConfigResponse update(@PathVariable Long id, @Valid @RequestBody SeasonalFeeConfigRequest req) {
        SeasonalFeeConfig config = load(id);
        Product product = productRepository.findById(req.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.getProductId()));
        config.setProduct(product);
        config.setBillingFrequency(req.getBillingFrequency());
        config.setAmount(req.getAmount());
        config.setNextDueDate(req.getNextDueDate());
        config.setPropertyReference(req.getPropertyReference());
        config.setDescription(req.getDescription());
        config.setActive(req.isActive());
        return toResponse(repository.save(config));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        SeasonalFeeConfig config = load(id);
        config.setActive(false);
        repository.save(config);
    }

    @PostMapping("/{id}/generate-now")
    public GenerateNowResponse generateNow(@PathVariable Long id) {
        SeasonalFeeConfig config = load(id);
        BillingEvent created = generationService.generateForConfig(config);
        return GenerateNowResponse.builder()
            .configId(config.getId())
            .billingEventId(created.getId())
            .eventDate(created.getEventDate())
            .amount(config.getAmount())
            .newNextDueDate(config.getNextDueDate())
            .message("Billing event created successfully. Next due date advanced to " + config.getNextDueDate() + ".")
            .build();
    }

    private SeasonalFeeConfig load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SeasonalFeeConfig not found: " + id));
    }

    private SeasonalFeeConfigResponse toResponse(SeasonalFeeConfig c) {
        return SeasonalFeeConfigResponse.builder()
            .id(c.getId())
            .customerNumber(c.getCustomerNumber())
            .productId(c.getProduct().getId())
            .productName(c.getProduct().getTranslations().stream()
                .filter(t -> "en".equals(t.getLocale())).findFirst()
                .map(t -> t.getName()).orElse(c.getProduct().getCode()))
            .billingFrequency(c.getBillingFrequency())
            .amount(c.getAmount())
            .nextDueDate(c.getNextDueDate())
            .propertyReference(c.getPropertyReference())
            .description(c.getDescription())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .createdBy(c.getCreatedBy())
            .build();
    }
}
