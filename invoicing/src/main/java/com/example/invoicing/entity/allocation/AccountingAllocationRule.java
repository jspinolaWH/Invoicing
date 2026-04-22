package com.example.invoicing.entity.allocation;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.account.PriceComponent;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

/**
 * Defines which AccountingAccount to use when billing a product in a given region/municipality.
 * The most specific rule wins — determined by specificityScore ORDER BY DESC.
 */
@Entity
@Table(name = "accounting_allocation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingAllocationRule extends BaseAuditEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String municipality;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id", nullable = false)
    private AccountingAccount accountingAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_component", length = 50)
    private PriceComponent priceComponent;

    /**
     * Specificity score: 1 = product only, 2 = product+region, 3 = product+region+municipality.
     * Computed by the service — not set by callers.
     */
    @Column(nullable = false)
    private Integer specificityScore;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
