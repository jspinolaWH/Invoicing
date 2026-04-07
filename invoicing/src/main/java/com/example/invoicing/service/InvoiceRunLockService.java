package com.example.invoicing.service;
import com.example.invoicing.common.exception.CustomerLockedException;

import com.example.invoicing.repository.ActiveRunLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceRunLockService {

    private final ActiveRunLockRepository lockRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void lockCustomers(Long runId, List<String> customerNumbers) {
        bulkInsertLocks(customerNumbers, runId);
        log.info("Locked {} customers for run {}", customerNumbers.size(), runId);
    }

    @Transactional
    public void releaseLocksForRun(Long runId) {
        lockRepository.deleteByRunId(runId);
        log.info("Released all locks for run {}", runId);
    }

    public void assertNotLocked(String customerNumber) {
        if (lockRepository.existsByCustomerNumber(customerNumber)) {
            com.example.invoicing.entity.invoicerun.ActiveRunLock lock = lockRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow();
            throw new CustomerLockedException(customerNumber, lock.getRunId(), lock.getLockedAt());
        }
    }

    public boolean isLocked(String customerNumber) {
        return lockRepository.existsByCustomerNumber(customerNumber);
    }

    private void bulkInsertLocks(List<String> customerNumbers, Long runId) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO active_run_locks (customer_number, run_id, locked_at, created_by, created_at) " +
            "VALUES (?, ?, ?, 'SYSTEM', ?)",
            customerNumbers,
            500,
            (ps, cn) -> {
                Timestamp now = Timestamp.from(Instant.now());
                ps.setString(1, cn);
                ps.setLong(2, runId);
                ps.setTimestamp(3, now);
                ps.setTimestamp(4, now);
            }
        );
    }
}
