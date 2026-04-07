package com.example.invoicing.entity.surcharge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalToggleResponse {
    private boolean globalSurchargeEnabled;
    private int updatedRecords;
}
