package com.example.invoicing.run.dto;
import lombok.Data;
import java.time.Instant;

@Data
public class ScheduleSendRequest {
    private Instant sendAt;
}
