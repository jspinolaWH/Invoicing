package com.example.invoicing.entity.wastetype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "waste_type_masters")
@Getter
@Setter
@NoArgsConstructor
public class WasteTypeMaster {

    @Id
    @Column(length = 50)
    private String code;

    @Column(name = "name_fi", nullable = false, length = 255)
    private String nameFi;

    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active;
}
