package com.example.invoicing.repository;

import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountingAllocationRuleRepository extends JpaRepository<AccountingAllocationRule, Long> {

    /**
     * Finds the best matching allocation rule for a product + optional location.
     * Full specificity-based matching is implemented in Step 19.
     * For now: returns the first rule for the product if any.
     */
    @Query("""
        SELECT r FROM AccountingAllocationRule r
        WHERE r.product.id = :productId
          AND (:locationId IS NULL OR r.locationId = :locationId OR r.locationId IS NULL)
        ORDER BY CASE WHEN r.locationId = :locationId THEN 0 ELSE 1 END, r.id ASC
        """)
    Optional<AccountingAllocationRule> findBestMatchForProduct(
        @Param("productId")  Long productId,
        @Param("locationId") String locationId
    );
}
