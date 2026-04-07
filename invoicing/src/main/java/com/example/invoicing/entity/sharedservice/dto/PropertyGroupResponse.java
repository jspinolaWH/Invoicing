package com.example.invoicing.entity.sharedservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class PropertyGroupResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private int participantCount;
    private BigDecimal totalSharePercentage;
    private boolean valid;
    private List<ParticipantResponse> participants;
}
