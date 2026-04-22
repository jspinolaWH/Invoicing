package com.example.invoicing.entity.invoice;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_classification", nullable = false)
    private LegalClassification legalClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id")
    private AccountingAccount accountingAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "bundled", nullable = false)
    private boolean bundled = false;

    @Column(name = "line_order")
    private Integer lineOrder;

    @ElementCollection
    @CollectionTable(name = "invoice_line_ledger_entries",
        joinColumns = @JoinColumn(name = "line_item_id"))
    private java.util.List<AccountingLedgerEntry> ledgerEntries = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_event_id")
    private BillingEvent sourceEvent;

    @Column(name = "waste_type", length = 100)
    private String wasteType;

    @Column(name = "shared_service_total_net", precision = 19, scale = 4)
    private BigDecimal sharedServiceTotalNet;
}
