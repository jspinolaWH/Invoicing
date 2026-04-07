package com.example.invoicing.finvoice;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
class VatGroup {
    BigDecimal vatRate;
    BigDecimal baseAmount;
    BigDecimal vatAmount;
}
