package com.example.invoicing.controller.sharedservice;
import com.example.invoicing.entity.sharedservice.dto.ValidationResultResponse;
import com.example.invoicing.entity.sharedservice.dto.ParticipantRequest;
import com.example.invoicing.entity.sharedservice.dto.PropertyGroupResponse;
import com.example.invoicing.entity.sharedservice.dto.PropertyGroupRequest;
import com.example.invoicing.common.exception.SharedServicePercentageException;
import com.example.invoicing.service.PropertyGroupService;

import com.example.invoicing.entity.sharedservice.dto.*;
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

    @PostMapping("/{id}/add-participant-retroactive")
    public PropertyGroupResponse addParticipantRetroactive(@PathVariable Long id,
                                                            @RequestBody RetroactiveParticipantRequest request) {
        return service.addParticipantRetroactive(id, request);
    }

    @ExceptionHandler(SharedServicePercentageException.class)
    public ResponseEntity<String> handlePercentageError(SharedServicePercentageException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
