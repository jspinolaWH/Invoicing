package com.example.invoicing.sharedservice;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "property_groups")
public class PropertyGroup extends BaseAuditEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "propertyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedServiceParticipant> participants = new ArrayList<>();
}
