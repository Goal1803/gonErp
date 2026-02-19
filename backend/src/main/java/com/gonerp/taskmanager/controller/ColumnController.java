package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.ColumnRequest;
import com.gonerp.taskmanager.dto.ColumnResponse;
import com.gonerp.taskmanager.dto.ReorderRequest;
import com.gonerp.taskmanager.service.ColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ColumnController {

    private final ColumnService columnService;

    @PostMapping("/api/tasks/boards/{boardId}/columns")
    public ResponseEntity<ApiResponse<ColumnResponse>> create(
            @PathVariable Long boardId, @Valid @RequestBody ColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Column created successfully", columnService.create(boardId, request)));
    }

    @PutMapping("/api/tasks/columns/{id}")
    public ResponseEntity<ApiResponse<ColumnResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ColumnRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Column updated successfully", columnService.update(id, request)));
    }

    @DeleteMapping("/api/tasks/columns/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        columnService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Column deleted successfully", null));
    }

    @PatchMapping("/api/tasks/boards/{boardId}/columns/reorder")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @PathVariable Long boardId, @RequestBody ReorderRequest request) {
        columnService.reorder(boardId, request);
        return ResponseEntity.ok(ApiResponse.ok("Columns reordered", null));
    }
}
