package com.example.invoicing.repository;
import com.example.invoicing.entity.sharedservice.SharedServiceParticipant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SharedServiceParticipantRepository extends JpaRepository<SharedServiceParticipant, Long> {

    @Query("SELECT p FROM SharedServiceParticipant p WHERE p.propertyGroup.id = :groupId " +
           "AND p.validFrom <= :date AND (p.validTo IS NULL OR p.validTo >= :date)")
    List<SharedServiceParticipant> findActiveAtDate(
        @Param("groupId") Long groupId,
        @Param("date") LocalDate date);

    @Query("SELECT SUM(p.sharePercentage) FROM SharedServiceParticipant p WHERE p.propertyGroup.id = :groupId " +
           "AND p.validFrom <= :today AND (p.validTo IS NULL OR p.validTo >= :today)")
    BigDecimal sumActiveSharePercentages(
        @Param("groupId") Long groupId,
        @Param("today") LocalDate today);

    List<SharedServiceParticipant> findByPropertyGroupId(Long groupId);
}
