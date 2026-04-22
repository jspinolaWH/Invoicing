package com.example.invoicing.entity.project.dto;

import com.example.invoicing.entity.project.Project;
import lombok.*;

import java.time.Instant;

@Data @Builder
public class ProjectResponse {
    private Long id;
    private String customerNumber;
    private String name;
    private String description;
    private boolean active;
    private Instant createdAt;

    public static ProjectResponse from(Project p) {
        return ProjectResponse.builder()
            .id(p.getId())
            .customerNumber(p.getCustomerNumber())
            .name(p.getName())
            .description(p.getDescription())
            .active(p.isActive())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
