package com.example.invoicing.seasonalfee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SeasonalFeeConfigRepository extends JpaRepository<SeasonalFeeConfig, Long> {

    @Query("SELECT s FROM SeasonalFeeConfig s WHERE s.active = true AND s.nextDueDate <= :today")
    List<SeasonalFeeConfig> findDueForGeneration(@Param("today") LocalDate today);

    List<SeasonalFeeConfig> findByCustomerNumberOrderByNextDueDateAsc(String customerNumber);

    List<SeasonalFeeConfig> findAllByActiveTrueOrderByNextDueDateAsc();
}
