package com.example.invoicing.sharedservice;

import com.example.invoicing.sharedservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property-groups")
@RequiredArgsConstructor
public class PropertyGroupController {

    private final PropertyGroupService service;

    @GetMapping
    public List<PropertyGroupResponse> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyGroupResponse create(@RequestBody PropertyGroupRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public PropertyGroupResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}/participants")
    public PropertyGroupResponse replaceParticipants(@PathVariable Long id,
                                                      @RequestBody List<ParticipantRequest> participants) {
        return service.replaceParticipants(id, participants);
    }

    @GetMapping("/{id}/validate")
    public ValidationResultResponse validate(@PathVariable Long id) {
        return service.validate(id);
    }

    @ExceptionHandler(SharedServicePercentageException.class)
    public ResponseEntity<String> handlePercentageError(SharedServicePercentageException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
