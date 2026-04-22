package com.example.invoicing.service;

import com.example.invoicing.entity.project.Project;
import com.example.invoicing.entity.project.dto.ProjectCreateRequest;
import com.example.invoicing.entity.project.dto.ProjectResponse;
import com.example.invoicing.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectResponse> listByCustomer(String customerNumber) {
        return projectRepository.findByCustomerNumber(customerNumber).stream()
            .map(ProjectResponse::from).toList();
    }

    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream()
            .map(ProjectResponse::from).toList();
    }

    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(findById(id));
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest req) {
        Project project = new Project();
        project.setCustomerNumber(req.getCustomerNumber());
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setActive(true);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectCreateRequest req) {
        Project project = findById(id);
        project.setCustomerNumber(req.getCustomerNumber());
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void deactivate(Long id) {
        Project project = findById(id);
        project.setActive(false);
        projectRepository.save(project);
    }

    private Project findById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }
}
