package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.BillingEventValidationStatus;
import com.example.invoicing.entity.classification.LegalClassification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillingEventRepository extends JpaRepository<BillingEvent, Long> {

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.customerNumber = :customerNumber
          AND e.eventDate >= :from
          AND e.eventDate <= :to
          AND e.status = 'IN_PROGRESS'
          AND e.excluded = false
        """)
    List<BillingEvent> findUnbilledByCustomerAndDateRange(
        @Param("customerNumber") String customerNumber,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.excluded = false
          AND (:municipalityId IS NULL OR e.municipalityId = :municipalityId)
          AND (:dateFrom     IS NULL OR e.eventDate >= :dateFrom)
          AND (:dateTo       IS NULL OR e.eventDate <= :dateTo)
          AND (:productId    IS NULL OR e.product.id = :productId)
          AND (:customerType IS NULL OR e.legalClassification = :customerType)
          AND (:locationId   IS NULL OR e.locationId = :locationId)
        """)
    List<BillingEvent> findByRunFilter(
        @Param("municipalityId")  String municipalityId,
        @Param("dateFrom")        LocalDate dateFrom,
        @Param("dateTo")          LocalDate dateTo,
        @Param("productId")       Long productId,
        @Param("customerType")    LegalClassification customerType,
        @Param("locationId")      String locationId
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.officeReviewRequired = true
          AND e.reviewedAt IS NULL
          AND e.excluded = false
        ORDER BY e.eventDate ASC
        """)
    List<BillingEvent> findPendingOfficeReview();

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.sharedServiceGroupId = :groupId
          AND e.status = 'IN_PROGRESS'
          AND e.eventDate >= :effectiveFrom
          AND e.excluded = false
        """)
    List<BillingEvent> findBySharedServiceGroup(
        @Param("groupId")       String groupId,
        @Param("effectiveFrom") LocalDate effectiveFrom
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.projectId = :projectId
          AND e.status = 'IN_PROGRESS'
          AND e.excluded = false
        """)
    List<BillingEvent> findByProject(@Param("projectId") String projectId);

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.customerNumber = :customerNumber
          AND e.status = 'IN_PROGRESS'
          AND e.eventDate >= :cycleStart
          AND e.eventDate <= :cycleEnd
          AND e.excluded = false
        """)
    List<BillingEvent> findByCycleWindow(
        @Param("customerNumber") String customerNumber,
        @Param("cycleStart")     LocalDate cycleStart,
        @Param("cycleEnd")       LocalDate cycleEnd
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.excluded = true
          AND (:customerNumber IS NULL OR e.customerNumber = :customerNumber)
          AND (:from IS NULL OR e.eventDate >= :from)
          AND (:to   IS NULL OR e.eventDate <= :to)
        ORDER BY e.excludedAt DESC
        """)
    List<BillingEvent> findExcluded(
        @Param("customerNumber") String customerNumber,
        @Param("from")           LocalDate from,
        @Param("to")             LocalDate to
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.excluded = false
          AND (e.accountingAccount IS NULL
               OR e.costCenter IS NULL
               OR e.vatRate0 IS NULL
               OR e.vatRate24 IS NULL
               OR e.customerNumber IS NULL
               OR e.product IS NULL)
        """)
    List<BillingEvent> findWithMissingMandatoryData();

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE (:customerNumber        IS NULL OR e.customerNumber = :customerNumber)
          AND (:status                IS NULL OR e.status = :status)
          AND (:municipalityId        IS NULL OR e.municipalityId = :municipalityId)
          AND (:dateFrom              IS NULL OR e.eventDate >= :dateFrom)
          AND (:dateTo                IS NULL OR e.eventDate <= :dateTo)
          AND (:productId             IS NULL OR e.product.id = :productId)
          AND (:excluded              IS NULL OR e.excluded = :excluded)
          AND (:requiresReview        IS NULL OR e.officeReviewRequired = :requiresReview)
          AND (:serviceResponsibility IS NULL OR e.serviceResponsibility = :serviceResponsibility)
          AND (:validationStatus      IS NULL OR e.validationStatus = :validationStatus)
          AND (:origin                IS NULL OR e.origin = :origin)
        ORDER BY e.eventDate DESC
        """)
    Page<BillingEvent> findFiltered(
        @Param("customerNumber")        String customerNumber,
        @Param("status")                BillingEventStatus status,
        @Param("municipalityId")        String municipalityId,
        @Param("dateFrom")              LocalDate dateFrom,
        @Param("dateTo")                LocalDate dateTo,
        @Param("productId")             Long productId,
        @Param("excluded")              Boolean excluded,
        @Param("requiresReview")        Boolean requiresReview,
        @Param("serviceResponsibility") String serviceResponsibility,
        @Param("validationStatus")      BillingEventValidationStatus validationStatus,
        @Param("origin")                String origin,
        Pageable pageable
    );

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.customerNumber = :customerNumber
          AND e.eventDate >= :from
          AND e.eventDate <= :to
          AND e.excluded = false
          AND (:productId IS NULL OR e.product.id = :productId)
          AND (:serviceResponsibility IS NULL OR e.serviceResponsibility = :serviceResponsibility)
        ORDER BY e.eventDate ASC
        """)
    List<BillingEvent> findByCustomerAndPeriod(
        @Param("customerNumber") String customerNumber,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("productId") Long productId,
        @Param("serviceResponsibility") String serviceResponsibility
    );

    @Query("""
        SELECT DISTINCT li.sourceEvent FROM InvoiceLineItem li
        WHERE li.invoice.id = :invoiceId AND li.sourceEvent IS NOT NULL
        """)
    List<BillingEvent> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.excluded = false
          AND e.product.code IN :productCodes
        """)
    List<BillingEvent> findUninvoicedByProductCodes(@Param("productCodes") List<String> productCodes);

    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.eventDate >= :dateFrom
          AND e.eventDate <= :dateTo
          AND (:status         IS NULL OR e.status = :status)
          AND (:customerNumber IS NULL OR e.customerNumber = :customerNumber)
          AND (:municipalityId IS NULL OR e.municipalityId = :municipalityId)
        ORDER BY e.eventDate ASC
        """)
    List<BillingEvent> findForExport(
        @Param("dateFrom")       LocalDate dateFrom,
        @Param("dateTo")         LocalDate dateTo,
        @Param("status")         BillingEventStatus status,
        @Param("customerNumber") String customerNumber,
        @Param("municipalityId") String municipalityId
    );
}
