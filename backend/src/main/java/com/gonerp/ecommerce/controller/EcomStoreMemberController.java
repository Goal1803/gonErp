package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomStoreMemberRequest;
import com.gonerp.ecommerce.dto.EcomStoreMemberResponse;
import com.gonerp.ecommerce.service.EcomStoreMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/stores/{storeId}/members")
@RequiredArgsConstructor
public class EcomStoreMemberController {

    private final EcomStoreMemberService ecomStoreMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomStoreMemberResponse>>> findByStore(
            @PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.ok(ecomStoreMemberService.findByStore(storeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EcomStoreMemberResponse>> assign(
            @PathVariable Long storeId, @Valid @RequestBody EcomStoreMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Member assigned", ecomStoreMemberService.assign(storeId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long storeId, @PathVariable Long userId) {
        ecomStoreMemberService.remove(storeId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed", null));
    }
}
