package com.gonerp.notes.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.notes.dto.*;
import com.gonerp.notes.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteSummaryResponse>>> tree() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getTree()));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<NoteSummaryResponse>>> favorites() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getFavorites()));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NoteSummaryResponse>>> recent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getRecent(limit)));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<NoteSummaryResponse>>> templates() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getTemplates()));
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<NoteSummaryResponse>>> trash() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getTrash()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<NoteSearchResponse>>> search(
            @RequestParam String q, @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.search(q, limit)));
    }

    /** Feeds the "@" picker in the editor. */
    @GetMapping("/mentions")
    public ResponseEntity<ApiResponse<List<NoteRefResponse>>> mentions(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.mentionCandidates(q, limit)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteDetailResponse>> create(
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note created successfully", noteService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.update(id, request)));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResponse<Void>> move(
            @PathVariable Long id, @RequestBody NoteMoveRequest request) {
        noteService.move(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Note moved successfully", null));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<NoteDetailResponse>> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note duplicated successfully", noteService.duplicate(id)));
    }

    /** Soft delete — moves the note and its children to the trash. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long id) {
        noteService.archive(id);
        return ResponseEntity.ok(ApiResponse.ok("Note moved to trash", null));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        noteService.restore(id);
        return ResponseEntity.ok(ApiResponse.ok("Note restored", null));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> deleteForever(@PathVariable Long id) {
        noteService.deleteForever(id);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted permanently", null));
    }

    @DeleteMapping("/trash")
    public ResponseEntity<ApiResponse<Void>> emptyTrash() {
        noteService.emptyTrash();
        return ResponseEntity.ok(ApiResponse.ok("Trash emptied", null));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(@PathVariable Long id) {
        boolean favorite = noteService.toggleFavorite(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorite", favorite)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<NoteMemberResponse>>> members(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getMembers(id)));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<NoteMemberResponse>> addMember(
            @PathVariable Long id, @RequestBody NoteMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Member added successfully", noteService.addMember(id, request)));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id, @PathVariable Long userId) {
        noteService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }
}
