package com.example.invoicing.entity.account.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AccountingAccountRequest {
    private String code;
    private String name;
    private LocalDate validFrom;
    private LocalDate validTo;
}
