package com.example.invoicing.entity.billingevent;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "billing_event_templates")
@Getter @Setter @NoArgsConstructor
public class BillingEventTemplate extends BaseAuditEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "customer_number", length = 9)
    private String customerNumber;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "waste_fee_price", precision = 19, scale = 4)
    private BigDecimal wasteFeePrice;

    @Column(name = "transport_fee_price", precision = 19, scale = 4)
    private BigDecimal transportFeePrice;

    @Column(name = "eco_fee_price", precision = 19, scale = 4)
    private BigDecimal ecoFeePrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal weight;

    @Column(name = "vehicle_id", length = 50)
    private String vehicleId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "location_id", length = 100)
    private String locationId;

    @Column(name = "municipality_id", length = 100)
    private String municipalityId;

    @Column(length = 2000)
    private String comments;

    @Column(length = 200)
    private String contractor;

    @Column(length = 20)
    private String direction;

    @Column(name = "shared_service_group_percentage", precision = 7, scale = 4)
    private BigDecimal sharedServiceGroupPercentage;

    @Column(name = "waste_type", length = 100)
    private String wasteType;

    @Column(name = "receiving_site", length = 200)
    private String receivingSite;
}
