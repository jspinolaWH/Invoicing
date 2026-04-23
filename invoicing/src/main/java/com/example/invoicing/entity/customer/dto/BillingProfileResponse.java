package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.*;
import lombok.*;
import java.time.Instant;

@Getter @Builder
public class BillingProfileResponse {
    private Long customerId;
    private String customerName;
    private CustomerType customerType;
    private BillingProfileDto billingProfile;
    private String lastModifiedBy;
    private Instant lastModifiedAt;
    private String parentCustomerNumber;
    private Integer openInvoicesUpdated;

    public static BillingProfileResponse from(Customer c) {
        return BillingProfileResponse.builder()
            .customerId(c.getId()).customerName(c.getName())
            .customerType(c.getCustomerType())
            .billingProfile(BillingProfileDto.from(c.getBillingProfile()))
            .lastModifiedBy(c.getLastModifiedBy()).lastModifiedAt(c.getLastModifiedAt())
            .parentCustomerNumber(c.getParentCustomerNumber())
            .build();
    }

    public static BillingProfileResponse fromWithCount(Customer c, int openInvoicesUpdated) {
        return BillingProfileResponse.builder()
            .customerId(c.getId()).customerName(c.getName())
            .customerType(c.getCustomerType())
            .billingProfile(BillingProfileDto.from(c.getBillingProfile()))
            .lastModifiedBy(c.getLastModifiedBy()).lastModifiedAt(c.getLastModifiedAt())
            .parentCustomerNumber(c.getParentCustomerNumber())
            .openInvoicesUpdated(openInvoicesUpdated)
            .build();
    }
}
