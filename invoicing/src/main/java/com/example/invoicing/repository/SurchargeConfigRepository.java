package com.example.invoicing.repository;
import com.example.invoicing.entity.surcharge.SurchargeConfig;

import com.example.invoicing.entity.customer.DeliveryMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SurchargeConfigRepository extends JpaRepository<SurchargeConfig, Long> {

    Optional<SurchargeConfig> findByDeliveryMethodAndActiveTrue(DeliveryMethod deliveryMethod);

    @Modifying
    @Transactional
    @Query("UPDATE SurchargeConfig s SET s.globalSurchargeEnabled = :enabled")
    int setGlobalSurchargeEnabled(@Param("enabled") boolean enabled);
}
