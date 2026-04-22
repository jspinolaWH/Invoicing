package com.example.invoicing.repository;

import com.example.invoicing.entity.account.PriceComponent;
import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountingAllocationRuleRepository extends JpaRepository<AccountingAllocationRule, Long> {

    /**
     * Find all active rules for a product/region/municipality/priceComponent, ordered by specificity DESC.
     * A null priceComponent param matches only catch-all (null-component) rules.
     * A non-null priceComponent param matches both specific and catch-all rules; the specific rule wins via higher score.
     * Callers take the first element — that is the most specific match.
     */
    @Query("""
        SELECT r FROM AccountingAllocationRule r
        WHERE r.product.id = :productId
          AND r.active = true
          AND (r.region IS NULL OR r.region = :region)
          AND (r.municipality IS NULL OR r.municipality = :municipality)
          AND (r.priceComponent IS NULL OR r.priceComponent = :priceComponent)
        ORDER BY r.specificityScore DESC
        """)
    List<AccountingAllocationRule> findMostSpecificRules(
        @Param("productId")      Long productId,
        @Param("region")         String region,
        @Param("municipality")   String municipality,
        @Param("priceComponent") PriceComponent priceComponent
    );

    /** Backward-compatible lookup used by BillingEventService.resolveAccountingDefaults(). */
    default Optional<AccountingAllocationRule> findBestMatchForProduct(Long productId, String regionOrLocation) {
        List<AccountingAllocationRule> rules = findMostSpecificRules(
            productId, regionOrLocation, regionOrLocation, null);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
    }

    List<AccountingAllocationRule> findAllByOrderBySpecificityScoreDesc();

    List<AccountingAllocationRule> findByProduct_IdAndActiveTrue(Long productId);
}
