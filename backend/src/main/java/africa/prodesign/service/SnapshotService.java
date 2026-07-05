package africa.prodesign.service;

import africa.prodesign.dto.SnapshotRequest;
import africa.prodesign.dto.SnapshotResponse;
import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.repository.DrawingSnapshotRepository;
import africa.prodesign.validation.GeometryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final DrawingSnapshotRepository snapshotRepository;
    private final ProjectService projectService;
    private final GeometryValidator geometryValidator;

    public SnapshotResponse latest(String userId, String projectId) {
        projectService.requireAccess(userId, projectId);
        DrawingSnapshot snapshot = snapshotRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .orElse(null);
        return snapshot == null ? null : toResponse(snapshot);
    }

    @Transactional
    public SnapshotResponse save(String userId, String projectId, SnapshotRequest request) {
        projectService.requireEditAccess(userId, projectId);
        geometryValidator.validate(request.canvasStateJson(), request.schemaVersion());
        DrawingSnapshot snapshot = DrawingSnapshot.builder()
                .projectId(projectId)
                .schemaVersion(request.schemaVersion())
                .canvasStateJson(request.canvasStateJson())
                .createdByUserId(userId)
                .isAutoSave(request.isAutoSave() == null || request.isAutoSave())
                .build();
        return toResponse(snapshotRepository.save(snapshot));
    }

    public List<SnapshotResponse> history(String userId, String projectId) {
        projectService.requireAccess(userId, projectId);
        return snapshotRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SnapshotResponse restore(String userId, String projectId, String versionId) {
        projectService.requireEditAccess(userId, projectId);
        DrawingSnapshot version = snapshotRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));
        if (!version.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Version does not belong to this project");
        }
        DrawingSnapshot restored = DrawingSnapshot.builder()
                .projectId(projectId)
                .schemaVersion(version.getSchemaVersion())
                .canvasStateJson(version.getCanvasStateJson())
                .createdByUserId(userId)
                .isAutoSave(false)
                .build();
        return toResponse(snapshotRepository.save(restored));
    }

    private SnapshotResponse toResponse(DrawingSnapshot s) {
        return new SnapshotResponse(s.getId(), s.getProjectId(), s.getSchemaVersion(),
                s.getCanvasStateJson(), s.getCreatedByUserId(), s.isAutoSave(), s.getCreatedAt());
    }
}
