package com.example.invoicing.repository;

import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountingAllocationRuleRepository extends JpaRepository<AccountingAllocationRule, Long> {

    /**
     * Find all active rules for a product/region/municipality, ordered by specificity DESC.
     * Callers take the first element — that is the most specific match.
     */
    @Query("""
        SELECT r FROM AccountingAllocationRule r
        WHERE r.product.id = :productId
          AND r.active = true
          AND (r.region IS NULL OR r.region = :region)
          AND (r.municipality IS NULL OR r.municipality = :municipality)
        ORDER BY r.specificityScore DESC
        """)
    List<AccountingAllocationRule> findMostSpecificRules(
        @Param("productId")    Long productId,
        @Param("region")       String region,
        @Param("municipality") String municipality
    );

    /** Backward-compatible lookup used by BillingEventService.resolveAccountingDefaults(). */
    default Optional<AccountingAllocationRule> findBestMatchForProduct(Long productId, String regionOrLocation) {
        List<AccountingAllocationRule> rules = findMostSpecificRules(
            productId, regionOrLocation, regionOrLocation);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
    }

    List<AccountingAllocationRule> findAllByOrderBySpecificityScoreDesc();

    List<AccountingAllocationRule> findByProduct_IdAndActiveTrue(Long productId);
}
