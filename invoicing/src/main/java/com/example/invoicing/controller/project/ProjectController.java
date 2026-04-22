package com.example.invoicing.controller.project;

import com.example.invoicing.entity.project.dto.ProjectCreateRequest;
import com.example.invoicing.entity.project.dto.ProjectResponse;
import com.example.invoicing.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(
            @RequestParam(required = false) String customerNumber) {
        List<ProjectResponse> result = customerNumber != null && !customerNumber.isBlank()
            ? projectService.listByCustomer(customerNumber)
            : projectService.listAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@RequestBody @Valid ProjectCreateRequest req) {
        return ResponseEntity.ok(projectService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ProjectCreateRequest req) {
        return ResponseEntity.ok(projectService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        projectService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
