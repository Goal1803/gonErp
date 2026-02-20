package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardSummaryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(boardService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BoardSummaryResponse>> create(@Valid @RequestBody BoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Board created successfully", boardService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardSummaryResponse>> update(
            @PathVariable Long id, @RequestBody BoardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Board updated successfully", boardService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        boardService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Board deleted successfully", null));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Long id, @RequestBody BoardMemberRequest request) {
        boardService.addMember(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable Long id, @PathVariable Long userId, @RequestBody BoardMemberRequest request) {
        boardService.updateMemberRole(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Member role updated successfully", null));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id, @PathVariable Long userId) {
        boardService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }

    @GetMapping("/{id}/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getLabels(id)));
    }

    @PostMapping("/{id}/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @PathVariable Long id, @RequestBody LabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Label created successfully", boardService.createLabel(id, request)));
    }

    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(@PathVariable Long labelId) {
        boardService.deleteLabel(labelId);
        return ResponseEntity.ok(ApiResponse.ok("Label deleted successfully", null));
    }

    @GetMapping("/{id}/types")
    public ResponseEntity<ApiResponse<List<TypeResponse>>> getTypes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getTypes(id)));
    }

    @PostMapping("/{id}/types")
    public ResponseEntity<ApiResponse<TypeResponse>> createType(
            @PathVariable Long id, @RequestBody TypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Type created successfully", boardService.createType(id, request)));
    }

    @DeleteMapping("/types/{typeId}")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long typeId) {
        boardService.deleteType(typeId);
        return ResponseEntity.ok(ApiResponse.ok("Type deleted successfully", null));
    }
}
