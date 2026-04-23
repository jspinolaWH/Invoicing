package com.example.invoicing.entity.receivingsite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "receiving_sites")
@Getter
@Setter
@NoArgsConstructor
public class ReceivingSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "municipality_name", nullable = false, length = 255)
    private String municipalityName;

    @Column(length = 255)
    private String address;

    @Column(name = "site_type", nullable = false, length = 100)
    private String siteType;

    @Column(nullable = false)
    private boolean active;
}
