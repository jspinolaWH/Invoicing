package com.example.invoicing.integration;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TransmissionStatusResponse {
    private Long invoiceId;
    private String invoiceNumber;
    private String externalReference;
    private ExternalDeliveryStatus deliveryStatus;
    private Instant transmittedAt;
}
