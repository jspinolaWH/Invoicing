package com.example.invoicing.entity.account;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "accounting_accounts")
public class AccountingAccount extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column
    private LocalDate validTo;
}
