package com.example.invoicing.entity.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    @Column(name = "vehicle_id", length = 20)
    private String vehicleId;

    @Column(name = "registration_plate", nullable = false, length = 20)
    private String registrationPlate;

    @Column(name = "vehicle_type", nullable = false, length = 100)
    private String vehicleType;

    @Column(nullable = false)
    private boolean active;
}
