package com.example.invoicing.entity.billingevent.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class BulkTransferRequest {

    @NotEmpty
    private List<Long> eventIds;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Target customer number must be 6–9 digits")
    private String targetCustomerNumber;

    private String targetPropertyId;

    @NotBlank(message = "A reason is required for bulk transfer")
    private String reason;
}
