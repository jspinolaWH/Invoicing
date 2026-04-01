package com.example.invoicing.entity.account.dto;

import com.example.invoicing.entity.account.AccountingAccount;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class AccountingAccountResponse {
    private Long id;
    private String code;
    private String name;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String createdBy;
    private Instant createdAt;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static AccountingAccountResponse from(AccountingAccount a) {
        AccountingAccountResponse r = new AccountingAccountResponse();
        r.id = a.getId();
        r.code = a.getCode();
        r.name = a.getName();
        r.validFrom = a.getValidFrom();
        r.validTo = a.getValidTo();
        r.createdBy = a.getCreatedBy();
        r.createdAt = a.getCreatedAt();
        r.lastModifiedBy = a.getLastModifiedBy();
        r.lastModifiedAt = a.getLastModifiedAt();
        return r;
    }
}
