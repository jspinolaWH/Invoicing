package com.example.invoicing.entity.invoicetemplate;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice_templates")
public class InvoiceTemplate extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "number_series_id")
    private InvoiceNumberSeries numberSeries;
}
