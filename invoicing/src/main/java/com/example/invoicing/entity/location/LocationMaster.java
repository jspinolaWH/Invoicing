package com.example.invoicing.entity.location;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location_masters")
@Getter
@Setter
@NoArgsConstructor
public class LocationMaster {

    @Id
    @Column(name = "location_id", length = 50)
    private String locationId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "municipality_id", nullable = false, length = 50)
    private String municipalityId;

    @Column(name = "municipality_name", nullable = false, length = 255)
    private String municipalityName;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private boolean active;
}
