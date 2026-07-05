package africa.prodesign.service;

import africa.prodesign.dto.ProjectRequest;
import africa.prodesign.dto.ProjectResponse;
import africa.prodesign.entity.Project;
import africa.prodesign.entity.ProjectCollaborator;
import africa.prodesign.enums.CollabRole;
import africa.prodesign.repository.ProjectCollaboratorRepository;
import africa.prodesign.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCollaboratorRepository collaboratorRepository;

    public List<ProjectResponse> listForUser(String userId) {
        List<Project> owned = projectRepository.findByOwnerId(userId);
        List<String> collabProjectIds = collaboratorRepository.findByUserId(userId).stream()
                .map(ProjectCollaborator::getProjectId)
                .toList();
        List<Project> collabProjects = projectRepository.findAllById(collabProjectIds);
        return Stream.concat(owned.stream(), collabProjects.stream())
                .distinct()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse create(String userId, ProjectRequest request) {
        Project project = Project.builder()
                .name(request.name())
                .location(request.location())
                .description(request.description())
                .complianceRuleCode(request.complianceRuleCode())
                .plotWidthMeters(request.plotWidthMeters())
                .plotDepthMeters(request.plotDepthMeters())
                .ownerId(userId)
                .build();
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(String userId, String projectId, ProjectRequest request) {
        Project project = requireEditableProject(userId, projectId);
        project.setName(request.name());
        project.setLocation(request.location());
        project.setDescription(request.description());
        project.setComplianceRuleCode(request.complianceRuleCode());
        project.setPlotWidthMeters(request.plotWidthMeters());
        project.setPlotDepthMeters(request.plotDepthMeters());
        project.setUpdatedAt(Instant.now());
        return toResponse(projectRepository.save(project));
    }

    private Project requireEditableProject(String userId, String projectId) {
        requireEditAccess(userId, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }

    public ProjectResponse get(String userId, String projectId) {
        Project project = requireAccess(userId, projectId);
        return toResponse(project);
    }

    @Transactional
    public void delete(String userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Only the owner can delete this project");
        }
        projectRepository.deleteById(projectId);
    }

    /** Loads a project and verifies the given user has at least VIEWER access (owner or collaborator). */
    public Project requireAccess(String userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getOwnerId().equals(userId)) {
            return project;
        }
        collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this project"));
        return project;
    }

    /** Verifies the given user has EDITOR or OWNER access. */
    public CollabRole requireEditAccess(String userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getOwnerId().equals(userId)) {
            return CollabRole.OWNER;
        }
        ProjectCollaborator collab = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this project"));
        if (collab.getRole() == CollabRole.VIEWER) {
            throw new AccessDeniedException("Viewers cannot make changes to this project");
        }
        return collab.getRole();
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getLocation(), p.getDescription(),
                p.getOwnerId(), p.getComplianceRuleCode(), p.getPlotWidthMeters(), p.getPlotDepthMeters(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
