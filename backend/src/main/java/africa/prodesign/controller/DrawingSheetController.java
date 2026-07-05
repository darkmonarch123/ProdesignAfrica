package africa.prodesign.controller;

import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.entity.Project;
import africa.prodesign.entity.User;
import africa.prodesign.repository.DrawingSnapshotRepository;
import africa.prodesign.repository.UserRepository;
import africa.prodesign.service.DrawingSheetPdfExportService;
import africa.prodesign.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DrawingSheetController {

    private final DrawingSheetPdfExportService exportService;
    private final ProjectService projectService;
    private final DrawingSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;

    @GetMapping("/api/projects/{projectId}/drawing-sheet/export.pdf")
    public ResponseEntity<byte[]> export(
            Authentication auth,
            @PathVariable String projectId,
            @RequestParam(defaultValue = "A3") String paperSize,
            @RequestParam(defaultValue = "100") int scale,
            @RequestParam(defaultValue = "Ground Floor Plan") String sheetTitle
    ) {
        String userId = (String) auth.getPrincipal();
        Project project = projectService.requireAccess(userId, projectId);
        DrawingSnapshot snapshot = snapshotRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No drawing found for this project yet"));
        String drawnBy = userRepository.findById(userId).map(User::getFullName).orElse(null);

        byte[] bytes = exportService.export(project, snapshot, paperSize, scale, sheetTitle, drawnBy);

        String safeName = project.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("Drawing_" + safeName + ".pdf").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(bytes);
    }
}
