package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayRequest;
import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.service.PublicHolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public-holidays")
@RequiredArgsConstructor
@Tag(name = "Public Holidays", description = "Company-wide holiday calendar")
@SecurityRequirement(name = "bearerAuth")
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create public holiday")
    public ResponseEntity<ApiResponse<PublicHolidayResponse>> create(@Valid @RequestBody PublicHolidayRequest request) {
        PublicHolidayResponse response = publicHolidayService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Public holiday created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update public holiday")
    public ResponseEntity<ApiResponse<PublicHolidayResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PublicHolidayRequest request) {
        PublicHolidayResponse response = publicHolidayService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Public holiday updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Delete public holiday")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        publicHolidayService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Public holiday deleted", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public holiday by id")
    public ResponseEntity<ApiResponse<PublicHolidayResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(publicHolidayService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List all public holidays")
    public ResponseEntity<ApiResponse<List<PublicHolidayResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(publicHolidayService.listAll()));
    }

    @GetMapping("/range")
    @Operation(summary = "List public holidays in a date range")
    public ResponseEntity<ApiResponse<List<PublicHolidayResponse>>> listByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                publicHolidayService.listByDateRange(startDate, endDate)));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "List public holidays by calendar year")
    public ResponseEntity<ApiResponse<List<PublicHolidayResponse>>> listByYear(@PathVariable int year) {
        return ResponseEntity.ok(ApiResponse.success(publicHolidayService.listByYear(year)));
    }
}
