package com.example.invoicing.controller.validation;
import com.example.invoicing.entity.validation.dto.*;
import com.example.invoicing.service.ValidationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/validation-rules")
@RequiredArgsConstructor
public class ValidationRuleController {
    private final ValidationRuleService service;

    @GetMapping
    public List<ValidationRuleResponse> list(@RequestParam Long companyId) {
        return service.findAll(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ValidationRuleResponse create(@RequestBody @Valid ValidationRuleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ValidationRuleResponse update(@PathVariable Long id, @RequestBody @Valid ValidationRuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
