package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.dto.PriceAdjustmentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceAdjustmentAuditLogRepository extends JpaRepository<PriceAdjustmentAuditLog, Long> {
}
