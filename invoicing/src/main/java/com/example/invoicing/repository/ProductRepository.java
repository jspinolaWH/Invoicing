package com.example.invoicing.repository;

import com.example.invoicing.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.translations t WHERE t.locale = :locale")
    List<Product> findByLocale(@Param("locale") String locale);
}
