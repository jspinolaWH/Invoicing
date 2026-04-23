package com.example.invoicing.entity.company;

import com.example.invoicing.entity.customer.InvoicingMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_invoicing_defaults")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyInvoicingDefaults {

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_invoicing_mode", nullable = false, length = 10)
    private InvoicingMode defaultInvoicingMode;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
