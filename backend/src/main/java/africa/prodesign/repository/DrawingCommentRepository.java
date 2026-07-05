package africa.prodesign.repository;

import africa.prodesign.entity.DrawingComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawingCommentRepository extends JpaRepository<DrawingComment, String> {
    List<DrawingComment> findByProjectIdOrderByCreatedAtAsc(String projectId);
}
