package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/columns/{columnId}/cards")
    public ResponseEntity<ApiResponse<CardDetailResponse>> create(
            @PathVariable Long columnId, @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Card created successfully", cardService.create(columnId, request)));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.findById(id)));
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> update(
            @PathVariable Long id, @RequestBody CardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Card updated", cardService.update(id, request)));
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Card deleted successfully", null));
    }

    @PatchMapping("/cards/{id}/move")
    public ResponseEntity<ApiResponse<Void>> move(
            @PathVariable Long id, @RequestBody CardMoveRequest request) {
        cardService.moveCard(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Card moved successfully", null));
    }

    @PatchMapping("/columns/{columnId}/cards/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderCards(
            @PathVariable Long columnId, @RequestBody ReorderRequest request) {
        cardService.reorderCards(columnId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cards reordered", null));
    }

    @PostMapping("/cards/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long id, @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Comment added", cardService.addComment(id, request)));
    }

    @DeleteMapping("/cards/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id, @PathVariable Long commentId) {
        cardService.deleteComment(id, commentId);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted", null));
    }

    @PostMapping(value = "/cards/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attachment uploaded", cardService.uploadAttachment(id, file, name)));
    }

    @DeleteMapping("/cards/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId) {
        cardService.deleteAttachment(id, attachmentId);
        return ResponseEntity.ok(ApiResponse.ok("Attachment deleted", null));
    }

    @PostMapping("/cards/{id}/links")
    public ResponseEntity<ApiResponse<LinkResponse>> addLink(
            @PathVariable Long id, @RequestBody LinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Link added", cardService.addLink(id, request)));
    }

    @DeleteMapping("/cards/{id}/links/{linkId}")
    public ResponseEntity<ApiResponse<Void>> deleteLink(
            @PathVariable Long id, @PathVariable Long linkId) {
        cardService.deleteLink(id, linkId);
        return ResponseEntity.ok(ApiResponse.ok("Link deleted", null));
    }

    @PostMapping("/cards/{id}/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> addLabel(
            @PathVariable Long id, @PathVariable Long labelId) {
        cardService.addLabel(id, labelId);
        return ResponseEntity.ok(ApiResponse.ok("Label added", null));
    }

    @DeleteMapping("/cards/{id}/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> removeLabel(
            @PathVariable Long id, @PathVariable Long labelId) {
        cardService.removeLabel(id, labelId);
        return ResponseEntity.ok(ApiResponse.ok("Label removed", null));
    }

    @PostMapping("/cards/{id}/types/{typeId}")
    public ResponseEntity<ApiResponse<Void>> addType(
            @PathVariable Long id, @PathVariable Long typeId) {
        cardService.addType(id, typeId);
        return ResponseEntity.ok(ApiResponse.ok("Type added", null));
    }

    @DeleteMapping("/cards/{id}/types/{typeId}")
    public ResponseEntity<ApiResponse<Void>> removeType(
            @PathVariable Long id, @PathVariable Long typeId) {
        cardService.removeType(id, typeId);
        return ResponseEntity.ok(ApiResponse.ok("Type removed", null));
    }

    @PostMapping("/cards/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Long id, @PathVariable Long userId) {
        cardService.addCardMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member added to card", null));
    }

    @DeleteMapping("/cards/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id, @PathVariable Long userId) {
        cardService.removeCardMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed from card", null));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource resource = cardService.loadFileAsResource(filename);
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(filename));
            if (contentType == null) contentType = "application/octet-stream";
        } catch (IOException ignored) {
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
