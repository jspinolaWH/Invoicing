package com.example.invoicing.entity.invoice;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class VatGroup {
    BigDecimal vatRate;
    BigDecimal baseAmount;
    BigDecimal vatAmount;
}
