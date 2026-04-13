package com.example.invoicing.controller.product;

import com.example.invoicing.entity.product.dto.InvoicingProductResponse;
import com.example.invoicing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoicing/products")
@RequiredArgsConstructor
public class InvoicingProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public List<InvoicingProductResponse> listActive() {
        return productRepository.findAllActive().stream()
                .map(InvoicingProductResponse::from)
                .toList();
    }
}
