package com.example.invoicing.surcharge.dto;

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
    private BigDecimal amount;
    private String description;
    private boolean active;
    private boolean globalSurchargeEnabled;
    private Instant createdAt;
    private String createdBy;
}
