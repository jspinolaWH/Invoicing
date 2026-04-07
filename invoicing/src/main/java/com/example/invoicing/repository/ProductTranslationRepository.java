package com.example.invoicing.repository;

import com.example.invoicing.entity.product.ProductTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, Long> {

    List<ProductTranslation> findByProductId(Long productId);

    Optional<ProductTranslation> findByProductIdAndLocale(Long productId, String locale);
}
