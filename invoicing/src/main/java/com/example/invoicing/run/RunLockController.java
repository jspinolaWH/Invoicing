package com.example.invoicing.run;

import com.example.invoicing.entity.invoicerun.ActiveRunLock;
import com.example.invoicing.repository.ActiveRunLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/run-locks")
@RequiredArgsConstructor
public class RunLockController {

    private final ActiveRunLockRepository lockRepository;

    @GetMapping("/check/{customerNumber}")
    public Map<String, Object> checkLock(@PathVariable String customerNumber) {
        Optional<ActiveRunLock> lock = lockRepository.findByCustomerNumber(customerNumber);
        if (lock.isPresent()) {
            return Map.of(
                "customerNumber", customerNumber,
                "locked", true,
                "runId", lock.get().getRunId(),
                "lockedAt", lock.get().getLockedAt().toString()
            );
        }
        return Map.of("customerNumber", customerNumber, "locked", false);
    }
}
