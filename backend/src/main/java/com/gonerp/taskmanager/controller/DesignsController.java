package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.DesignSummaryResponse;
import com.gonerp.taskmanager.service.DesignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks/designs")
@RequiredArgsConstructor
public class DesignsController {

    private final DesignsService designsService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DesignSummaryResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long sellerId,
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
                designsService.findAll(boardId, sellerId, designerId,
                        productTypeId, nicheId, occasionId,
                        stage, status, custom, dateFrom, dateTo,
                        search, pageable)));
    }
}
