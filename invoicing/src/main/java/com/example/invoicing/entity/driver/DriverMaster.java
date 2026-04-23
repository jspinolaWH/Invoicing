package com.example.invoicing.entity.driver;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class DriverMaster {

    @Id
    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(nullable = false)
    private boolean active;
}
