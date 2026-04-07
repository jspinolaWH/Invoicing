package com.example.invoicing.driver;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "event_type_configs")
@Getter
@Setter
@NoArgsConstructor
public class EventTypeConfig extends BaseAuditEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String eventTypeCode;

    @Column(nullable = false)
    private boolean requiresOfficeReview;

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualQuantityThreshold;

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualWeightThreshold;

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualPriceThreshold;

    @Column(nullable = false)
    private boolean reviewIfUnknownLocation = true;

    @Column(length = 500)
    private String description;
}
