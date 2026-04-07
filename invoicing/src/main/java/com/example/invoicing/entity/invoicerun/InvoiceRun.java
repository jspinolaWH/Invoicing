package com.example.invoicing.entity.invoicerun;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "invoice_runs")
public class InvoiceRun extends BaseAuditEntity {

    @Column(name = "run_label", nullable = false, length = 255)
    private String runLabel;

    @Column(name = "billing_period_from")
    private LocalDate billingPeriodFrom;

    @Column(name = "billing_period_to")
    private LocalDate billingPeriodTo;

    @Column(name = "simulation_mode", nullable = false)
    private boolean simulationMode = false;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";
}
