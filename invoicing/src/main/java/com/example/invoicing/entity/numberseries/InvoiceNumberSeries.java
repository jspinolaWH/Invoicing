package com.example.invoicing.entity.numberseries;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoice_number_series")
public class InvoiceNumberSeries extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String name;            // e.g. "MAIN_2026", "CREDIT_2026"

    @Column(nullable = false)
    private String prefix;          // e.g. "INV", "CR"

    @Column(nullable = false)
    private String formatPattern;   // e.g. "{PREFIX}-{YEAR}-{COUNTER:06d}"

    @Column(nullable = false)
    private Long currentCounter;    // monotonically increasing — never decremented

    // Released numbers are never re-issued — kept for auditing only
    @ElementCollection
    @CollectionTable(name = "released_invoice_numbers", joinColumns = @JoinColumn(name = "series_id"))
    @Column(name = "released_number")
    private List<String> releasedNumbers = new ArrayList<>();
}
