package com.example.invoicing.entity.contract;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contracts")
@Getter @Setter @NoArgsConstructor
public class Contract extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 9)
    private String customerNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "contract_products",
        joinColumns = @JoinColumn(name = "contract_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();

    @Column(name = "invoice_template_id")
    private Long invoiceTemplateId;

    @Column(name = "work_site", length = 200)
    private String workSite;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
