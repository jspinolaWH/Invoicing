package com.example.invoicing.entity.invoicerun.dto;
import lombok.Data;
import java.time.Instant;

@Data
public class ScheduleSendRequest {
    private Instant sendAt;
}
