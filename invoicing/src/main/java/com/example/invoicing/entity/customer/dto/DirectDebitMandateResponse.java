package com.example.invoicing.entity.customer.dto;

import com.example.invoicing.entity.customer.DirectDebitMandate;
import lombok.*;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectDebitMandateResponse {
    private Long customerId;
    private String mandateReference;
    private String bankAccount;
    private Instant activatedAt;
    private Instant terminatedAt;

    public static DirectDebitMandateResponse from(DirectDebitMandate m) {
        return DirectDebitMandateResponse.builder()
            .customerId(m.getCustomer().getId())
            .mandateReference(m.getMandateReference())
            .bankAccount(m.getBankAccount())
            .activatedAt(m.getActivatedAt())
            .terminatedAt(m.getTerminatedAt())
            .build();
    }

    public static DirectDebitMandateResponse empty(Long customerId) {
        return DirectDebitMandateResponse.builder()
            .customerId(customerId)
            .build();
    }
}
