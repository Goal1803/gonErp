package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import com.gonerp.taskmanager.service.DesignDetailService;
import com.gonerp.taskmanager.service.DesignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks/designs")
@RequiredArgsConstructor
public class DesignsController {

    private final DesignsService designsService;
    private final DesignDetailService designDetailService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DesignSummaryResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long ideaCreatorId,
            @RequestParam(required = false) Long designerId,
            @RequestParam(required = false) Long productTypeId,
            @RequestParam(required = false) Long nicheId,
            @RequestParam(required = false) Long occasionId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean custom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.ok(
                designsService.findAll(boardId, ideaCreatorId, designerId,
                        productTypeId, nicheId, occasionId,
                        stage, status, custom, dateFrom, dateTo,
                        search, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DesignSummaryResponse>> createDesign(
            @RequestBody CreateDesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Design created", designsService.createDesign(request)));
    }

    // ── Design-ID-based endpoints ──────────────────────────────────────────

    @GetMapping("/{designId}/detail")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> getDesignDetail(
            @PathVariable Long designId) {
        return ResponseEntity.ok(ApiResponse.ok(designDetailService.getDesignDetailById(designId)));
    }

    @PutMapping("/{designId}")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> updateDesignDetail(
            @PathVariable Long designId, @RequestBody DesignDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Design updated",
                designDetailService.updateDesignDetailById(designId, request)));
    }

    @PostMapping("/{designId}/png-files")
    public ResponseEntity<ApiResponse<DesignFileResponse>> uploadPngFile(
            @PathVariable Long designId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("PNG file uploaded",
                        designDetailService.uploadDesignFileById(designId, file, DesignFileCategory.PNG)));
    }

    @DeleteMapping("/{designId}/png-files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deletePngFile(
            @PathVariable Long designId, @PathVariable Long fileId) {
        designDetailService.deleteDesignFileById(designId, fileId);
        return ResponseEntity.ok(ApiResponse.ok("PNG file removed", null));
    }

    @PostMapping("/{designId}/psd-files")
    public ResponseEntity<ApiResponse<DesignFileResponse>> uploadPsdFile(
            @PathVariable Long designId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("PSD file uploaded",
                        designDetailService.uploadDesignFileById(designId, file, DesignFileCategory.PSD)));
    }

    @DeleteMapping("/{designId}/psd-files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deletePsdFile(
            @PathVariable Long designId, @PathVariable Long fileId) {
        designDetailService.deleteDesignFileById(designId, fileId);
        return ResponseEntity.ok(ApiResponse.ok("PSD file removed", null));
    }

    @PostMapping("/{designId}/mockups")
    public ResponseEntity<ApiResponse<DesignMockupResponse>> uploadMockup(
            @PathVariable Long designId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Mockup uploaded",
                        designDetailService.uploadMockupById(designId, file)));
    }

    @DeleteMapping("/{designId}/mockups/{mockupId}")
    public ResponseEntity<ApiResponse<Void>> deleteMockup(
            @PathVariable Long designId, @PathVariable Long mockupId) {
        designDetailService.deleteMockupById(designId, mockupId);
        return ResponseEntity.ok(ApiResponse.ok("Mockup deleted", null));
    }

    @PatchMapping("/{designId}/mockups/{mockupId}/main")
    public ResponseEntity<ApiResponse<DesignMockupResponse>> setMainMockup(
            @PathVariable Long designId, @PathVariable Long mockupId) {
        return ResponseEntity.ok(ApiResponse.ok("Main mockup set",
                designDetailService.setMainMockupById(designId, mockupId)));
    }

    @PostMapping("/{designId}/designers/{userId}")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> addDesigner(
            @PathVariable Long designId, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Designer added",
                designDetailService.addDesignerById(designId, userId)));
    }

    @DeleteMapping("/{designId}/designers/{userId}")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> removeDesigner(
            @PathVariable Long designId, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Designer removed",
                designDetailService.removeDesignerById(designId, userId)));
    }

    @DeleteMapping("/{designId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDesign(@PathVariable Long designId) {
        designDetailService.deleteDesignById(designId);
        return ResponseEntity.ok(ApiResponse.ok("Design deleted", null));
    }
}
