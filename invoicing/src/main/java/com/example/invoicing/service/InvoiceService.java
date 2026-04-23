package com.example.invoicing.service;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.InvoicingMode;
import com.example.invoicing.entity.invoice.InvoiceLanguage;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.entity.invoice.InvoiceType;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.reportingaudit.ReportingDataAuditLog;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CompanyInvoicingDefaultsRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.InvoiceTemplateRepository;
import com.example.invoicing.repository.ReportingDataAuditLogRepository;

import com.example.invoicing.entity.invoice.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository repository;
    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceTemplateRepository invoiceTemplateRepository;
    private final ReportingDataAuditLogRepository reportingAuditLogRepository;
    private final CompanyInvoicingDefaultsRepository companyInvoicingDefaultsRepository;

    public InvoiceResponse findById(Long id) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        return toResponse(invoice);
    }

    public List<InvoiceResponse> findByRunId(Long runId) {
        return repository.findByInvoiceRunId(runId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Page<InvoiceResponse> findAll(String billingType, Pageable pageable) {
        String bt = (billingType == null || billingType.isBlank()) ? null : billingType;
        return repository.findFiltered(bt, pageable).map(this::toResponse);
    }

    @Transactional
    public InvoiceResponse updateCustomText(Long id, String customText) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        invoice.setCustomText(customText);
        Invoice saved = repository.save(invoice);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedBy = (auth != null && auth.getName() != null) ? auth.getName() : "system";
        reportingAuditLogRepository.save(ReportingDataAuditLog.builder()
            .invoiceId(saved.getId())
            .invoiceNumber(saved.getInvoiceNumber())
            .lineItemId(null)
            .field("customText")
            .assignedValue(customText != null ? customText : "")
            .loggedAt(Instant.now())
            .loggedBy(loggedBy)
            .build());
        return toResponse(saved);
    }

    @Transactional
    public void bulkUpdateCustomText(List<Long> ids, String customText) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedBy = (auth != null && auth.getName() != null) ? auth.getName() : "system";
        Instant now = Instant.now();
        for (Long id : ids) {
            repository.findById(id).ifPresent(inv -> {
                inv.setCustomText(customText);
                Invoice saved = repository.save(inv);
                reportingAuditLogRepository.save(ReportingDataAuditLog.builder()
                    .invoiceId(saved.getId())
                    .invoiceNumber(saved.getInvoiceNumber())
                    .lineItemId(null)
                    .field("customText")
                    .assignedValue(customText != null ? customText : "")
                    .loggedAt(now)
                    .loggedBy(loggedBy)
                    .build());
            });
        }
    }

    @Transactional
    public InvoiceResponse removeSurcharge(Long id) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        invoice.getLineItems().removeIf(li -> li.getDescription() != null
            && li.getDescription().startsWith("Invoicing surcharge"));
        return toResponse(repository.save(invoice));
    }

    public List<Invoice> findOpenByCustomerId(Long customerId) {
        return repository.findOpenByCustomerId(customerId);
    }

    public Page<InvoiceResponse> findSentOrCompletedByCustomerId(Long customerId, Pageable pageable) {
        return repository.findSentOrCompletedByCustomerId(customerId, pageable).map(this::toResponse);
    }

    public Page<InvoiceResponse> findCreditNotesByCustomerId(Long customerId, Pageable pageable) {
        return repository.findCreditNotesByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional
    public Long createDraftFromEvents(List<Long> billingEventIds, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        List<BillingEvent> events = billingEventIds.isEmpty()
            ? List.of()
            : billingEventRepository.findAllById(billingEventIds);

        boolean invoicePerProject = customer.getBillingProfile() != null
            && customer.getBillingProfile().isInvoicePerProject();

        if (invoicePerProject && !events.isEmpty()) {
            Map<String, List<BillingEvent>> byProject = new LinkedHashMap<>();
            for (BillingEvent ev : events) {
                String key = ev.getProjectId() != null ? ev.getProjectId() : "";
                byProject.computeIfAbsent(key, k -> new ArrayList<>()).add(ev);
            }
            Long firstId = null;
            for (Map.Entry<String, List<BillingEvent>> entry : byProject.entrySet()) {
                Long id = buildAndSaveDraft(customer, entry.getValue(), entry.getKey().isEmpty() ? null : entry.getKey());
                if (firstId == null) firstId = id;
            }
            return firstId;
        }

        return buildAndSaveDraft(customer, events, null);
    }

    private Long buildAndSaveDraft(Customer customer, List<BillingEvent> events, String projectReference) {
        InvoicingMode companyDefault = companyInvoicingDefaultsRepository.findById(1L)
            .map(d -> d.getDefaultInvoicingMode())
            .orElse(InvoicingMode.NET);
        InvoicingMode invoicingMode = customer.getBillingProfile() != null
            && customer.getBillingProfile().getInvoicingMode() != null
            ? customer.getBillingProfile().getInvoicingMode()
            : companyDefault;

        InvoiceLanguage language = resolveLanguage(
            customer.getBillingProfile() != null ? customer.getBillingProfile().getLanguageCode() : null);

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceType(InvoiceType.STANDARD);
        invoice.setTemplateCode(resolveTemplateCode(customer));
        invoice.setLanguage(language);
        invoice.setInvoicingMode(invoicingMode);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(14));
        invoice.setProjectReference(projectReference);

        BigDecimal net = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO;

        for (BillingEvent ev : events) {
            BigDecimal unitPrice = ev.getWasteFeePrice()
                .add(ev.getTransportFeePrice())
                .add(ev.getEcoFeePrice());
            BigDecimal qty = ev.getQuantity();
            BigDecimal netAmt = qty.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP);
            BigDecimal vatRate = ev.getVatRate24() != null ? ev.getVatRate24() : BigDecimal.ZERO;
            BigDecimal grossAmt = netAmt.multiply(
                BigDecimal.ONE.add(vatRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
            ).setScale(4, RoundingMode.HALF_UP);

            LegalClassification cls = ev.getLegalClassification() != null
                ? ev.getLegalClassification()
                : LegalClassification.PRIVATE_LAW;

            String description = (ev.getProduct() != null ? ev.getProduct().getCode() : "Event")
                + " - " + ev.getEventDate();

            InvoiceLineItem li = new InvoiceLineItem();
            li.setInvoice(invoice);
            li.setProduct(ev.getProduct());
            li.setDescription(description);
            li.setQuantity(qty);
            li.setUnitPrice(unitPrice);
            li.setVatRate(vatRate);
            li.setNetAmount(netAmt);
            li.setGrossAmount(grossAmt);
            li.setLegalClassification(cls);
            li.setAccountingAccount(ev.getAccountingAccount());
            li.setCostCenter(ev.getCostCenter());
            li.setSourceEvent(ev);
            invoice.getLineItems().add(li);

            net = net.add(netAmt);
            gross = gross.add(grossAmt);
        }

        invoice.setNetAmount(net);
        invoice.setGrossAmount(gross);
        invoice.setVatAmount(gross.subtract(net));

        return repository.save(invoice).getId();
    }

    private String resolveTemplateCode(Customer customer) {
        Long templateId = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getInvoiceTemplateId() : null;
        if (templateId != null) {
            return invoiceTemplateRepository.findById(templateId)
                .map(t -> t.getCode())
                .orElse("WASTE_STANDARD");
        }
        return "WASTE_STANDARD";
    }

    private InvoiceLanguage resolveLanguage(String langCode) {
        if (langCode == null) return InvoiceLanguage.FI;
        return switch (langCode.toLowerCase()) {
            case "sv" -> InvoiceLanguage.SV;
            case "en" -> InvoiceLanguage.EN;
            default -> InvoiceLanguage.FI;
        };
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineItemResponse> lineItems = invoice.getLineItems().stream()
            .map(li -> InvoiceLineItemResponse.builder()
                .id(li.getId())
                .description(li.getDescription())
                .quantity(li.getQuantity())
                .unitPrice(li.getUnitPrice())
                .vatRate(li.getVatRate())
                .netAmount(li.getNetAmount())
                .grossAmount(li.getGrossAmount())
                .legalClassification(li.getLegalClassification() != null ? li.getLegalClassification().name() : null)
                .accountingAccountCode(li.getAccountingAccount() != null ? li.getAccountingAccount().getCode() : null)
                .costCenterCode(li.getCostCenter() != null ? li.getCostCenter().getCompositeCode() : null)
                .bundled(li.isBundled())
                .lineOrder(li.getLineOrder())
                .sharedServiceTotalNet(li.getSharedServiceTotalNet())
                .build())
            .collect(Collectors.toList());

        List<InvoiceAttachmentResponse> attachments = invoice.getAttachments().stream()
            .map(a -> InvoiceAttachmentResponse.builder()
                .id(a.getId())
                .attachmentIdentifier(a.getAttachmentIdentifier())
                .filename(a.getFilename())
                .mimeType(a.getMimeType())
                .sizeBytes(a.getSizeBytes())
                .securityClass(a.getSecurityClass())
                .build())
            .collect(Collectors.toList());

        List<CreditNoteSummary> creditNotes = repository.findCreditNotesByOriginalInvoiceId(invoice.getId()).stream()
            .map(cn -> CreditNoteSummary.builder()
                .creditNoteId(cn.getId())
                .creditNoteNumber(cn.getInvoiceNumber())
                .issuedAt(cn.getInvoiceDate())
                .netAmount(cn.getNetAmount())
                .build())
            .collect(Collectors.toList());

        return InvoiceResponse.builder()
            .id(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .templateCode(invoice.getTemplateCode())
            .language(invoice.getLanguage() != null ? invoice.getLanguage().name() : null)
            .invoicingMode(invoice.getInvoicingMode() != null ? invoice.getInvoicingMode().name() : null)
            .reverseChargeVat(invoice.isReverseChargeVat())
            .customText(invoice.getCustomText())
            .internalComment(invoice.getInternalComment())
            .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
            .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
            .originalInvoiceId(invoice.getOriginalInvoice() != null ? invoice.getOriginalInvoice().getId() : null)
            .invoiceDate(invoice.getInvoiceDate())
            .dueDate(invoice.getDueDate())
            .netAmount(invoice.getNetAmount())
            .grossAmount(invoice.getGrossAmount())
            .vatAmount(invoice.getVatAmount())
            .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
            .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
            .allowExternalRecall(invoice.getCustomer() != null
                && invoice.getCustomer().getBillingProfile() != null
                && invoice.getCustomer().getBillingProfile().isAllowExternalRecall())
            .billingType(invoice.getBillingType())
            .projectReference(invoice.getProjectReference())
            .lineItems(lineItems)
            .attachments(attachments)
            .creditNotes(creditNotes)
            .build();
    }
}
