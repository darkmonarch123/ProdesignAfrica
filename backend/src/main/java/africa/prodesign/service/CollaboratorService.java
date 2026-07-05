package africa.prodesign.service;

import africa.prodesign.dto.CollaboratorRequest;
import africa.prodesign.dto.CollaboratorResponse;
import africa.prodesign.entity.Project;
import africa.prodesign.entity.ProjectCollaborator;
import africa.prodesign.entity.User;
import africa.prodesign.enums.CollabRole;
import africa.prodesign.repository.ProjectCollaboratorRepository;
import africa.prodesign.repository.ProjectRepository;
import africa.prodesign.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final ProjectRepository projectRepository;
    private final ProjectCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public List<CollaboratorResponse> list(String requesterId, String projectId) {
        projectService.requireAccess(requesterId, projectId);
        return collaboratorRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CollaboratorResponse invite(String requesterId, String projectId, CollaboratorRequest request) {
        Project project = requireOwner(requesterId, projectId);
        User invitee = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No user found with that email"));
        if (invitee.getId().equals(project.getOwnerId())) {
            throw new IllegalArgumentException("The project owner is already a collaborator");
        }
        ProjectCollaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, invitee.getId())
                .orElse(ProjectCollaborator.builder().projectId(projectId).userId(invitee.getId()).build());
        collaborator.setRole(request.role() == CollabRole.OWNER ? CollabRole.EDITOR : request.role());
        return toResponse(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public CollaboratorResponse updateRole(String requesterId, String projectId, String userId, CollabRole role) {
        requireOwner(requesterId, projectId);
        ProjectCollaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        collaborator.setRole(role == CollabRole.OWNER ? CollabRole.EDITOR : role);
        return toResponse(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public void remove(String requesterId, String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        boolean isOwner = project.getOwnerId().equals(requesterId);
        boolean isSelfRemoval = requesterId.equals(userId);
        if (!isOwner && !isSelfRemoval) {
            throw new AccessDeniedException("Only the owner can remove other collaborators");
        }
        ProjectCollaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        collaboratorRepository.delete(collaborator);
    }

    private Project requireOwner(String requesterId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("Only the project owner can manage collaborators");
        }
        return project;
    }

    private CollaboratorResponse toResponse(ProjectCollaborator c) {
        User user = userRepository.findById(c.getUserId()).orElse(null);
        return new CollaboratorResponse(
                c.getUserId(),
                user != null ? user.getEmail() : "unknown",
                user != null ? user.getFullName() : "unknown",
                c.getRole(),
                c.getInvitedAt()
        );
    }
}
