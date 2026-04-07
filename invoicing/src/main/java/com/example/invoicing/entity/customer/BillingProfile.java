package com.example.invoicing.entity.customer;
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
}
