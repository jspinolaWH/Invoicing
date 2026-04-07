package com.example.invoicing.controller.product;

import com.example.invoicing.entity.product.dto.ProductRequest;
import com.example.invoicing.entity.product.dto.ProductResponse;
import com.example.invoicing.entity.product.dto.TranslationRequest;
import com.example.invoicing.entity.product.dto.TranslationResponse;
import com.example.invoicing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    public List<ProductResponse> getAll(@RequestParam(required = false) String locale) {
        if (locale != null) {
            return service.findByLocale(locale).stream().map(ProductResponse::from).toList();
        }
        return service.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody ProductRequest request) {
        return ProductResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ProductResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/translations")
    public List<TranslationResponse> getTranslations(@PathVariable Long id) {
        return service.findTranslations(id).stream().map(TranslationResponse::from).toList();
    }

    @PostMapping("/{id}/translations")
    public TranslationResponse upsertTranslation(
            @PathVariable Long id,
            @RequestBody TranslationRequest request) {
        return TranslationResponse.from(
                service.addOrUpdateTranslation(id, request.getLocale(), request.getName()));
    }

    @DeleteMapping("/{id}/translations/{locale}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTranslation(@PathVariable Long id, @PathVariable String locale) {
        service.deleteTranslation(id, locale);
    }
}
