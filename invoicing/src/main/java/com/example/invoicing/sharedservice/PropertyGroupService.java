package com.example.invoicing.sharedservice;

import com.example.invoicing.sharedservice.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyGroupService {

    private final PropertyGroupRepository groupRepository;
    private final SharedServiceParticipantRepository participantRepository;
    private final SharedServiceValidationService validationService;

    public List<PropertyGroupResponse> findAll() {
        return groupRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PropertyGroupResponse findById(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public PropertyGroupResponse create(PropertyGroupRequest request) {
        PropertyGroup group = new PropertyGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public PropertyGroupResponse replaceParticipants(Long groupId, List<ParticipantRequest> requests) {
        PropertyGroup group = load(groupId);
        group.getParticipants().clear();
        groupRepository.save(group);

        for (ParticipantRequest req : requests) {
            SharedServiceParticipant p = new SharedServiceParticipant();
            p.setPropertyGroup(group);
            p.setCustomerNumber(req.getCustomerNumber());
            p.setSharePercentage(req.getSharePercentage().setScale(2, RoundingMode.HALF_UP));
            p.setValidFrom(req.getValidFrom());
            p.setValidTo(req.getValidTo());
            group.getParticipants().add(p);
        }
        groupRepository.save(group);
        validationService.validateTotalEquals100(groupId);
        return toResponse(load(groupId));
    }

    public ValidationResultResponse validate(Long groupId) {
        BigDecimal total = validationService.getTotalSharePercentage(groupId);
        boolean valid = validationService.isTotalValid(groupId);
        return ValidationResultResponse.builder()
            .groupId(groupId)
            .totalSharePercentage(total)
            .valid(valid)
            .message(valid ? null : "Participant shares sum to " + total + "% but must equal exactly 100.00%")
            .build();
    }

    private PropertyGroup load(Long id) {
        return groupRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PropertyGroup not found: " + id));
    }

    private PropertyGroupResponse toResponse(PropertyGroup group) {
        BigDecimal total = validationService.getTotalSharePercentage(group.getId() != null ? group.getId() : 0L);
        List<ParticipantResponse> participants = group.getParticipants().stream()
            .map(p -> ParticipantResponse.builder()
                .id(p.getId())
                .customerNumber(p.getCustomerNumber())
                .sharePercentage(p.getSharePercentage())
                .validFrom(p.getValidFrom())
                .validTo(p.getValidTo())
                .build())
            .collect(Collectors.toList());

        return PropertyGroupResponse.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .active(group.isActive())
            .participantCount(participants.size())
            .totalSharePercentage(total)
            .valid(validationService.isTotalValid(group.getId() != null ? group.getId() : 0L))
            .participants(participants)
            .build();
    }
}
