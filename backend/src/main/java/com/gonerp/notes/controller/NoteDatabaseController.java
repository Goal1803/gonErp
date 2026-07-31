package com.gonerp.notes.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.notes.dto.*;
import com.gonerp.notes.service.NoteDatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteDatabaseController {

    private final NoteDatabaseService databaseService;

    // ── the database itself ──

    @PostMapping("/{id}/database")
    public ResponseEntity<ApiResponse<DatabaseResponse>> convertToDatabase(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Converted to database",
                databaseService.convertToDatabase(id)));
    }

    @DeleteMapping("/{id}/database")
    public ResponseEntity<ApiResponse<Void>> convertToPage(@PathVariable Long id) {
        databaseService.convertToPage(id);
        return ResponseEntity.ok(ApiResponse.ok("Converted back to a page", null));
    }

    @GetMapping("/databases/{id}")
    public ResponseEntity<ApiResponse<DatabaseResponse>> schema(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(databaseService.getSchema(id)));
    }

    // ── properties (columns) ──

    @PostMapping("/databases/{id}/properties")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @PathVariable Long id, @RequestBody PropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property created", databaseService.createProperty(id, request)));
    }

    @PutMapping("/properties/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @PathVariable Long propertyId, @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(databaseService.updateProperty(propertyId, request)));
    }

    @DeleteMapping("/properties/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(@PathVariable Long propertyId) {
        databaseService.deleteProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.ok("Property deleted", null));
    }

    // ── views ──

    @PostMapping("/databases/{id}/views")
    public ResponseEntity<ApiResponse<ViewResponse>> createView(
            @PathVariable Long id, @RequestBody ViewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("View created", databaseService.createView(id, request)));
    }

    @PutMapping("/views/{viewId}")
    public ResponseEntity<ApiResponse<ViewResponse>> updateView(
            @PathVariable Long viewId, @RequestBody ViewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(databaseService.updateView(viewId, request)));
    }

    @DeleteMapping("/views/{viewId}")
    public ResponseEntity<ApiResponse<Void>> deleteView(@PathVariable Long viewId) {
        databaseService.deleteView(viewId);
        return ResponseEntity.ok(ApiResponse.ok("View deleted", null));
    }

    // ── rows ──

    @GetMapping("/databases/{id}/rows")
    public ResponseEntity<ApiResponse<RowsResponse>> rows(
            @PathVariable Long id, @RequestParam(required = false) Long viewId) {
        return ResponseEntity.ok(ApiResponse.ok(databaseService.getRows(id, viewId)));
    }

    @PostMapping("/databases/{id}/rows")
    public ResponseEntity<ApiResponse<RowResponse>> createRow(
            @PathVariable Long id, @RequestBody(required = false) RowValueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Row created", databaseService.createRow(id, request)));
    }

    @PatchMapping("/rows/{rowId}/values")
    public ResponseEntity<ApiResponse<RowResponse>> setValues(
            @PathVariable Long rowId, @RequestBody RowValueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(databaseService.setValues(rowId, request)));
    }
}
