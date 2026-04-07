package com.example.invoicing.entity.validation;
import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationReport {
    private int totalChecked;
    private int passed;
    private int blockingFailureCount;
    private int warningCount;
    private List<ValidationFailure> failures;

    public List<ValidationFailure> blockingFailures() {
        return failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).toList();
    }
    public List<ValidationFailure> warnings() {
        return failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.WARNING).toList();
    }
    public boolean hasBlockingFailures() {
        return failures.stream().anyMatch(f -> f.getSeverity() == ValidationSeverity.BLOCKING);
    }
}
