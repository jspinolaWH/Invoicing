package com.example.invoicing.entity.invoice;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.InvoicingMode;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "invoices",
    uniqueConstraints = @UniqueConstraint(columnNames = "invoice_number"))
public class Invoice extends BaseAuditEntity {

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_number_series_id")
    private InvoiceNumberSeries invoiceNumberSeries;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private InvoiceLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoicing_mode", nullable = false)
    private InvoicingMode invoicingMode;

    @Column(name = "reverse_charge_vat", nullable = false)
    private boolean reverseChargeVat = false;

    @Column(name = "custom_text", length = 2000)
    private String customText;

    @Column(name = "internal_comment", length = 2000)
    private String internalComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_run_id")
    private InvoiceRun invoiceRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType = InvoiceType.STANDARD;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "gross_amount", precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "vat_amount", precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceAttachment> attachments = new ArrayList<>();

    @Column(name = "scheduled_send_at")
    private Instant scheduledSendAt;

    @Lob
    @Column(name = "finvoice_xml")
    private String finvoiceXml;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "billing_type", length = 20)
    private String billingType;

    @Column(name = "project_reference", length = 200)
    private String projectReference;
}
