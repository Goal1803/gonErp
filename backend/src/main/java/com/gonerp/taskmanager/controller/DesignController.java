package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.DesignDetailRequest;
import com.gonerp.taskmanager.dto.DesignDetailResponse;
import com.gonerp.taskmanager.dto.DesignFileResponse;
import com.gonerp.taskmanager.dto.DesignMockupResponse;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import com.gonerp.taskmanager.service.DesignDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks/cards/{cardId}/design")
@RequiredArgsConstructor
public class DesignController {

    private final DesignDetailService designDetailService;

    @GetMapping
    public ResponseEntity<ApiResponse<DesignDetailResponse>> getDesignDetail(@PathVariable Long cardId) {
        return ResponseEntity.ok(ApiResponse.ok(designDetailService.getDesignDetail(cardId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DesignDetailResponse>> updateDesignDetail(
            @PathVariable Long cardId, @RequestBody DesignDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Design detail updated", designDetailService.updateDesignDetail(cardId, request)));
    }

    @PostMapping("/png-files")
    public ResponseEntity<ApiResponse<DesignFileResponse>> uploadPngFile(
            @PathVariable Long cardId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("PNG file uploaded", designDetailService.uploadDesignFile(cardId, file, DesignFileCategory.PNG)));
    }

    @DeleteMapping("/png-files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deletePngFile(
            @PathVariable Long cardId, @PathVariable Long fileId) {
        designDetailService.deleteDesignFile(cardId, fileId);
        return ResponseEntity.ok(ApiResponse.ok("PNG file removed", null));
    }

    @PostMapping("/psd-files")
    public ResponseEntity<ApiResponse<DesignFileResponse>> uploadPsdFile(
            @PathVariable Long cardId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("PSD file uploaded", designDetailService.uploadDesignFile(cardId, file, DesignFileCategory.PSD)));
    }

    @DeleteMapping("/psd-files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deletePsdFile(
            @PathVariable Long cardId, @PathVariable Long fileId) {
        designDetailService.deleteDesignFile(cardId, fileId);
        return ResponseEntity.ok(ApiResponse.ok("PSD file removed", null));
    }

    @PostMapping("/mockups")
    public ResponseEntity<ApiResponse<DesignMockupResponse>> uploadMockup(
            @PathVariable Long cardId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Mockup uploaded", designDetailService.uploadMockup(cardId, file)));
    }

    @DeleteMapping("/mockups/{mockupId}")
    public ResponseEntity<ApiResponse<Void>> deleteMockup(
            @PathVariable Long cardId, @PathVariable Long mockupId) {
        designDetailService.deleteMockup(cardId, mockupId);
        return ResponseEntity.ok(ApiResponse.ok("Mockup deleted", null));
    }

    @PatchMapping("/mockups/{mockupId}/main")
    public ResponseEntity<ApiResponse<DesignMockupResponse>> setMainMockup(
            @PathVariable Long cardId, @PathVariable Long mockupId) {
        return ResponseEntity.ok(ApiResponse.ok("Main mockup set", designDetailService.setMainMockup(cardId, mockupId)));
    }

    @PostMapping("/designers/{userId}")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> addDesigner(
            @PathVariable Long cardId, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Designer added", designDetailService.addDesigner(cardId, userId)));
    }

    @DeleteMapping("/designers/{userId}")
    public ResponseEntity<ApiResponse<DesignDetailResponse>> removeDesigner(
            @PathVariable Long cardId, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Designer removed", designDetailService.removeDesigner(cardId, userId)));
    }
}
