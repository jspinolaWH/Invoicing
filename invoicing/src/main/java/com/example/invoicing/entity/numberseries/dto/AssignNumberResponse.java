package com.example.invoicing.entity.numberseries.dto;

import lombok.Getter;

@Getter
public class AssignNumberResponse {
    private final String assignedNumber;
    private final Long seriesId;
    private final boolean simulationMode;

    public AssignNumberResponse(String assignedNumber, Long seriesId, boolean simulationMode) {
        this.assignedNumber = assignedNumber;
        this.seriesId = seriesId;
        this.simulationMode = simulationMode;
    }
}
