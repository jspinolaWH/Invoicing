package com.example.invoicing.entity.customer;
import com.example.invoicing.entity.classification.LegalClassification;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BillingProfile {
    @Column(name = "customer_id_number", length = 9) private String customerIdNumber;
    @Enumerated(EnumType.STRING) @Column(name = "delivery_method", length = 20) private DeliveryMethod deliveryMethod;
    @Embedded private BillingAddress billingAddress;
    @Column(name = "business_id", length = 20) private String businessId;
    @Column(name = "language_code", length = 5) private String languageCode;
    @Enumerated(EnumType.STRING) @Column(name = "invoicing_mode", length = 10) private InvoicingMode invoicingMode;
    @Column(name = "invoice_template_id") private Long invoiceTemplateId;
    @Enumerated(EnumType.STRING) @Column(name = "default_legal_classification", length = 20) private LegalClassification defaultLegalClassification;
    @Column(name = "default_ledger_code", length = 20) private String defaultLedgerCode;
    @Column(name = "invoice_per_project", nullable = false) private boolean invoicePerProject = false;
    @Column(name = "allow_external_recall", nullable = false) private boolean allowExternalRecall = false;
}
