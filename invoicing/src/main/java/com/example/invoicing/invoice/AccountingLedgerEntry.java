package com.example.invoicing.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AccountingLedgerEntry {

    @Column(name = "ledger_code", length = 20)
    private String ledgerCode;

    @Column(name = "ledger_description", length = 255)
    private String ledgerDescription;

    @Column(name = "ledger_amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "ledger_vat_amount", precision = 19, scale = 4)
    private BigDecimal vatAmount;
}
