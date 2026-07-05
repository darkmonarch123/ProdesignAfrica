package africa.prodesign.service;

import africa.prodesign.dto.*;
import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.entity.Project;
import africa.prodesign.entity.Template;
import africa.prodesign.repository.DrawingSnapshotRepository;
import africa.prodesign.repository.ProjectRepository;
import africa.prodesign.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final ProjectRepository projectRepository;
    private final DrawingSnapshotRepository snapshotRepository;

    public TemplatePageResponse list(Integer bedroomsFilter, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 60);
        Page<Template> result = bedroomsFilter != null
                ? templateRepository.findByBedrooms(bedroomsFilter, PageRequest.of(page, safeSize))
                : templateRepository.findAll(PageRequest.of(page, safeSize));

        return new TemplatePageResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public TemplateDetailResponse get(String templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        return toDetail(template);
    }

    @Transactional
    public ProjectResponse useTemplate(String userId, String templateId, UseTemplateRequest request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        Project project = Project.builder()
                .name(request.projectName())
                .ownerId(userId)
                .plotWidthMeters(request.plotWidthMeters() != null ? request.plotWidthMeters() : template.getSuggestedPlotWidthMeters())
                .plotDepthMeters(request.plotDepthMeters() != null ? request.plotDepthMeters() : template.getSuggestedPlotDepthMeters())
                .build();
        project = projectRepository.save(project);

        DrawingSnapshot snapshot = DrawingSnapshot.builder()
                .projectId(project.getId())
                .schemaVersion("1.1")
                .canvasStateJson(template.getCanvasStateJson())
                .createdByUserId(userId)
                .isAutoSave(false)
                .build();
        snapshotRepository.save(snapshot);

        return new ProjectResponse(project.getId(), project.getName(), project.getLocation(), project.getDescription(),
                project.getOwnerId(), project.getComplianceRuleCode(), project.getPlotWidthMeters(),
                project.getPlotDepthMeters(), project.getCreatedAt(), project.getUpdatedAt());
    }

    private TemplateSummaryResponse toSummary(Template t) {
        return new TemplateSummaryResponse(t.getId(), t.getName(), t.getCategory(), t.getBedrooms(), t.getStyle(),
                t.getSuggestedPlotWidthMeters(), t.getSuggestedPlotDepthMeters(), t.getThumbnailSvg());
    }

    private TemplateDetailResponse toDetail(Template t) {
        return new TemplateDetailResponse(t.getId(), t.getName(), t.getCategory(), t.getBedrooms(), t.getStyle(),
                t.getSuggestedPlotWidthMeters(), t.getSuggestedPlotDepthMeters(), t.getThumbnailSvg(), t.getCanvasStateJson());
    }
}
