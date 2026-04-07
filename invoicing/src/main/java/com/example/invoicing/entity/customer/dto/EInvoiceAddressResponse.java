package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.EInvoiceAddress;
import lombok.*;
import java.time.Instant;

@Getter @Builder
public class EInvoiceAddressResponse {
    private Long customerId;
    private String address;
    private String operatorCode;
    private boolean manuallyLocked;
    private String lockReason;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static EInvoiceAddressResponse from(EInvoiceAddress e) {
        return EInvoiceAddressResponse.builder()
            .customerId(e.getCustomer().getId()).address(e.getAddress())
            .operatorCode(e.getOperatorCode()).manuallyLocked(e.isManuallyLocked())
            .lockReason(e.getLockReason()).lastModifiedBy(e.getLastModifiedBy())
            .lastModifiedAt(e.getLastModifiedAt()).build();
    }

    public static EInvoiceAddressResponse empty(Long customerId) {
        return EInvoiceAddressResponse.builder().customerId(customerId).manuallyLocked(false).build();
    }
}
