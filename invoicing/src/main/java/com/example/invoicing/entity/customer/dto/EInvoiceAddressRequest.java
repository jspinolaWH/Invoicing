package com.example.invoicing.entity.customer.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class EInvoiceAddressRequest {
    @NotBlank(message = "address must not be blank") @Size(max = 35) private String address;
    @Size(max = 20) private String operatorCode;
    private boolean lock = false;
    @Size(max = 500) private String lockReason;
}
