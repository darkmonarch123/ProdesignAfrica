package africa.prodesign.controller;

import africa.prodesign.dto.BoqResponse;
import africa.prodesign.entity.Project;
import africa.prodesign.enums.Currency;
import africa.prodesign.service.BoqExcelExportService;
import africa.prodesign.service.BoqPdfExportService;
import africa.prodesign.service.BoqService;
import africa.prodesign.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/boq")
@RequiredArgsConstructor
public class BoqController {

    private final BoqService boqService;
    private final BoqExcelExportService excelExportService;
    private final BoqPdfExportService pdfExportService;
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<BoqResponse> preview(
            Authentication auth, @PathVariable String projectId,
            @RequestParam(defaultValue = "NGN") Currency currency
    ) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(boqService.compute(userId, projectId, currency));
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(
            Authentication auth, @PathVariable String projectId,
            @RequestParam(defaultValue = "NGN") Currency currency
    ) {
        String userId = (String) auth.getPrincipal();
        Project project = projectService.requireAccess(userId, projectId);
        BoqResponse boq = boqService.compute(userId, projectId, currency);
        byte[] bytes = excelExportService.export(project, boq);
        return fileResponse(bytes, project.getName(), "xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            Authentication auth, @PathVariable String projectId,
            @RequestParam(defaultValue = "NGN") Currency currency
    ) {
        String userId = (String) auth.getPrincipal();
        Project project = projectService.requireAccess(userId, projectId);
        BoqResponse boq = boqService.compute(userId, projectId, currency);
        byte[] bytes = pdfExportService.export(project, boq);
        return fileResponse(bytes, project.getName(), "pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String projectName, String extension, MediaType mediaType) {
        String safeName = projectName.replaceAll("[^a-zA-Z0-9-_]", "_");
        String filename = "BOQ_" + safeName + "." + extension;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(bytes);
    }
}
