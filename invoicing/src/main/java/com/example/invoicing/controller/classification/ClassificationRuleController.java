package com.example.invoicing.controller.classification;
import com.example.invoicing.entity.classification.dto.*;
import com.example.invoicing.service.ClassificationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/classification-rules")
@RequiredArgsConstructor
public class ClassificationRuleController {
    private final ClassificationRuleService service;

    @GetMapping
    public ResponseEntity<List<ClassificationRuleResponse>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.findAll(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationRuleResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClassificationRuleResponse> create(@RequestBody @Valid ClassificationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.getCompanyId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassificationRuleResponse> update(@PathVariable Long id, @RequestBody @Valid ClassificationRuleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<ClassificationRuleResponse>> reorder(@RequestBody @Valid ReorderRequest request) {
        return ResponseEntity.ok(service.reorder(request.getCompanyId(), request.getOrderedIds()));
    }
}
