package africa.prodesign.controller;

import africa.prodesign.dto.ProjectResponse;
import africa.prodesign.dto.TemplateDetailResponse;
import africa.prodesign.dto.TemplatePageResponse;
import africa.prodesign.dto.UseTemplateRequest;
import africa.prodesign.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<TemplatePageResponse> list(
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return ResponseEntity.ok(templateService.list(bedrooms, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDetailResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(templateService.get(id));
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<ProjectResponse> use(Authentication auth, @PathVariable String id,
                                                 @Valid @RequestBody UseTemplateRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.useTemplate(userId, id, request));
    }
}
