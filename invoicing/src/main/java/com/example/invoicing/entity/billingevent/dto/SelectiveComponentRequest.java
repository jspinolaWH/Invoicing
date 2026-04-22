package com.example.invoicing.entity.billingevent.dto;

import lombok.Data;

@Data
public class SelectiveComponentRequest {
    private Boolean includeWasteFee;
    private Boolean includeTransportFee;
    private Boolean includeEcoFee;
}
