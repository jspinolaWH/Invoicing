package com.example.invoicing.entity.surcharge.dto;

import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.customer.DeliveryMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SurchargeConfigRequest {
    @NotNull
    private DeliveryMethod deliveryMethod;
    private CustomerType customerType;
    @NotNull
    private BigDecimal amount;
    private String description;
    private boolean active = true;
    private boolean exemptFirstInvoice = false;
    private boolean requiresTariffInclusion = false;
}
