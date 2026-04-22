package com.example.invoicing.entity.project;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor
public class Project extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 9)
    private String customerNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
