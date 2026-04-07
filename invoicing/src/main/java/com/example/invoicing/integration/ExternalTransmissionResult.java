package com.example.invoicing.integration;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ExternalTransmissionResult {
    private String externalReference;
    private ExternalDeliveryStatus status;
    private Instant transmittedAt;
    private String operatorResponse;
}
