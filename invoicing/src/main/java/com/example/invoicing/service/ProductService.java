package com.example.invoicing.service;

import com.example.invoicing.entity.product.Product;
import com.example.invoicing.entity.product.ProductTranslation;
import com.example.invoicing.entity.product.dto.ProductRequest;
import com.example.invoicing.repository.ProductRepository;
import com.example.invoicing.repository.ProductTranslationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTranslationRepository translationRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByLocale(String locale) {
        return productRepository.findByLocale(locale);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product p = new Product();
        applyRequest(p, request);
        return productRepository.save(p);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product p = findById(id);
        applyRequest(p, request);
        return productRepository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findById(id));
    }

    public List<ProductTranslation> findTranslations(Long productId) {
        return translationRepository.findByProductId(productId);
    }

    @Transactional
    public ProductTranslation addOrUpdateTranslation(Long productId, String locale, String name) {
        Product product = findById(productId);
        ProductTranslation translation = translationRepository
                .findByProductIdAndLocale(productId, locale)
                .orElseGet(() -> {
                    ProductTranslation t = new ProductTranslation();
                    t.setProduct(product);
                    t.setLocale(locale);
                    return t;
                });
        translation.setName(name);
        return translationRepository.save(translation);
    }

    @Transactional
    public void deleteTranslation(Long productId, String locale) {
        ProductTranslation translation = translationRepository
                .findByProductIdAndLocale(productId, locale)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Translation not found for product " + productId + ", locale " + locale));
        translationRepository.delete(translation);
    }

    private void applyRequest(Product p, ProductRequest request) {
        p.setCode(request.getCode().toUpperCase().trim());
        p.setPricingUnit(request.getPricingUnit());
        p.setReverseChargeVat(request.isReverseChargeVat());
    }
}
