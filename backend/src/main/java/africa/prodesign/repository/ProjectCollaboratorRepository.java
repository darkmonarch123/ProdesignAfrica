package africa.prodesign.repository;

import africa.prodesign.entity.ProjectCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCollaboratorRepository extends JpaRepository<ProjectCollaborator, String> {
    List<ProjectCollaborator> findByProjectId(String projectId);
    Optional<ProjectCollaborator> findByProjectIdAndUserId(String projectId, String userId);
    List<ProjectCollaborator> findByUserId(String userId);
}
