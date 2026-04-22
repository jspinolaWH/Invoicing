package com.example.invoicing.entity.billingevent.retroactive;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "service_responsibility_change_log",
    indexes = {
        @Index(name = "idx_src_log_change_run",    columnList = "change_run_id"),
        @Index(name = "idx_src_log_from_customer", columnList = "from_customer_number"),
        @Index(name = "idx_src_log_applied_at",    columnList = "applied_at")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponsibilityChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_run_id", nullable = false, length = 36)
    private String changeRunId;

    @Column(name = "applied_by", nullable = false, length = 100)
    private String appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "from_customer_number", nullable = false, length = 9)
    private String fromCustomerNumber;

    @Column(name = "to_customer_number", nullable = false, length = 9)
    private String toCustomerNumber;

    @Column(name = "previous_responsibility", length = 100)
    private String previousResponsibility;

    @Column(name = "new_responsibility", length = 100)
    private String newResponsibility;

    @Column(name = "change_effective_date")
    private LocalDate changeEffectiveDate;

    @Column(name = "affected_count", nullable = false)
    private int affectedCount;

    @Column(name = "moved_in_progress_count", nullable = false)
    private int movedInProgressCount;

    @Column(name = "correction_copies_created", nullable = false)
    private int correctionCopiesCreated;

    @Column(length = 2000)
    private String reason;
}
