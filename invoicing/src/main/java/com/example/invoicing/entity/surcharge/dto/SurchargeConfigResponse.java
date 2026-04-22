package com.example.invoicing.entity.surcharge.dto;

import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.customer.DeliveryMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SurchargeConfigResponse {
    private Long id;
    private DeliveryMethod deliveryMethod;
    private CustomerType customerType;
    private BigDecimal amount;
    private String description;
    private boolean active;
    private boolean globalSurchargeEnabled;
    private boolean exemptFirstInvoice;
    private boolean requiresTariffInclusion;
    private Instant createdAt;
    private String createdBy;
}
