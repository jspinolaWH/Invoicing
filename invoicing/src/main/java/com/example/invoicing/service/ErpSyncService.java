package com.example.invoicing.service;

import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.repository.InvoiceLineItemRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErpSyncService {

    private final InvoiceLineItemRepository lineItemRepository;

    public List<ErpLineItem> exportLineItems(LocalDate from, LocalDate to) {
        return lineItemRepository.findAll().stream()
                .filter(li -> {
                    if (li.getInvoice() == null || li.getInvoice().getInvoiceDate() == null) return false;
                    LocalDate d = li.getInvoice().getInvoiceDate();
                    if (from != null && d.isBefore(from)) return false;
                    if (to != null && d.isAfter(to)) return false;
                    return true;
                })
                .map(li -> ErpLineItem.builder()
                        .invoiceId(li.getInvoice().getId())
                        .invoiceNumber(li.getInvoice().getInvoiceNumber())
                        .invoiceDate(li.getInvoice().getInvoiceDate())
                        .lineItemId(li.getId())
                        .description(li.getDescription())
                        .productCode(li.getProduct() != null ? li.getProduct().getCode() : null)
                        .accountCode(li.getAccountingAccount() != null ? li.getAccountingAccount().getCode() : null)
                        .accountName(li.getAccountingAccount() != null ? li.getAccountingAccount().getName() : null)
                        .costCenterCode(li.getCostCenter() != null ? li.getCostCenter().getCompositeCode() : null)
                        .netAmount(li.getNetAmount())
                        .vatRate(li.getVatRate())
                        .grossAmount(li.getGrossAmount())
                        .build())
                .toList();
    }

    @Value
    @Builder
    public static class ErpLineItem {
        Long invoiceId;
        String invoiceNumber;
        LocalDate invoiceDate;
        Long lineItemId;
        String description;
        String productCode;
        String accountCode;
        String accountName;
        String costCenterCode;
        BigDecimal netAmount;
        BigDecimal vatRate;
        BigDecimal grossAmount;
    }
}
