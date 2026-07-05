package africa.prodesign.repository;

import africa.prodesign.entity.DrawingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrawingSnapshotRepository extends JpaRepository<DrawingSnapshot, String> {
    List<DrawingSnapshot> findByProjectIdOrderByCreatedAtDesc(String projectId);
    Optional<DrawingSnapshot> findFirstByProjectIdOrderByCreatedAtDesc(String projectId);
}
