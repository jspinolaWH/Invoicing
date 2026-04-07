package com.example.invoicing.entity.allocation;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stub entity — full implementation in Step 19 (AccountingAllocationRule).
 * Exists here so BillingEventService can reference it for compilation.
 */
@Entity
@Table(name = "accounting_allocation_rules")
@Getter @Setter @NoArgsConstructor
public class AccountingAllocationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "location_id", length = 100)
    private String locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id")
    private AccountingAccount accountingAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;
}
