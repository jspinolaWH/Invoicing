package com.example.invoicing.recall;

import lombok.Data;

@Data
public class RecallRequest {
    private String reason;
    private String internalComment;
}
