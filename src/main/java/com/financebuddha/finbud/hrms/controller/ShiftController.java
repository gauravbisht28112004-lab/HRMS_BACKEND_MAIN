package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;
import com.financebuddha.finbud.hrms.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Shifts", description = "Shift management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create shift", description = "Create a new shift type")
    public ResponseEntity<ApiResponse<ShiftTypeResponse>> createShift(@Valid @RequestBody ShiftTypeRequest request) {
        ShiftTypeResponse response = shiftService.createShiftType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Shift created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update shift", description = "Update an existing shift type")
    public ResponseEntity<ApiResponse<ShiftTypeResponse>> updateShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftTypeRequest request) {
        ShiftTypeResponse response = shiftService.updateShiftType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Shift updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Delete shift", description = "Delete a shift type")
    public ResponseEntity<ApiResponse<Void>> deleteShift(@PathVariable Long id) {
        shiftService.deleteShiftType(id);
        return ResponseEntity.ok(ApiResponse.success("Shift deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shift by ID", description = "Get shift details by ID")
    public ResponseEntity<ApiResponse<ShiftTypeResponse>> getShiftById(@PathVariable Long id) {
        ShiftTypeResponse response = shiftService.getShiftTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all shifts", description = "Get all shift types")
    public ResponseEntity<ApiResponse<List<ShiftTypeResponse>>> getAllShifts() {
        List<ShiftTypeResponse> response = shiftService.getAllShiftTypes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Assign shift", description = "Assign shift to employee")
    public ResponseEntity<ApiResponse<Void>> assignShift(
            @RequestParam Long employeeId,
            @RequestParam Long shiftTypeId) {
        shiftService.assignShift(employeeId, shiftTypeId);
        return ResponseEntity.ok(ApiResponse.success("Shift assigned successfully", null));
    }
}
