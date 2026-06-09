package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationRequest;
import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.service.OfficeLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/office-locations")
@RequiredArgsConstructor
@Tag(name = "Office Locations", description = "Office location + geofence management (HR/Admin)")
@SecurityRequirement(name = "bearerAuth")
public class OfficeLocationController {

    private final OfficeLocationService officeLocationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create office location", description = "Register a physical office with geofence settings")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> create(@Valid @RequestBody OfficeLocationRequest request) {
        OfficeLocationResponse response = officeLocationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Office location created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update office location")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody OfficeLocationRequest request) {
        OfficeLocationResponse response = officeLocationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Office location updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Deactivate office location", description = "Soft-delete by flipping is_active to false")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        officeLocationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Office location deactivated", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get office location by id")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> getById(@PathVariable Long id) {
        OfficeLocationResponse response = officeLocationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "List all office locations")
    public ResponseEntity<ApiResponse<List<OfficeLocationResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(officeLocationService.listAll()));
    }

    @GetMapping("/active")
    @Operation(summary = "List active office locations",
            description = "Open to all authenticated users so the MarkAttendance card can show office choices")
    public ResponseEntity<ApiResponse<List<OfficeLocationResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(officeLocationService.listActive()));
    }
}
