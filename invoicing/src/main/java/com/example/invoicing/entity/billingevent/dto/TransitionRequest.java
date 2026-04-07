package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.billingevent.BillingEventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransitionRequest {
    @NotNull
    private BillingEventStatus targetStatus;
}
