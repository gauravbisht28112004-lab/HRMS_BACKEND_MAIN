package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentResponse;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;
import com.financebuddha.finbud.hrms.service.ShiftService;
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
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Shifts", description = "Shift management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ShiftController {

    private final ShiftService shiftService;

    // ==================== Shift types ====================

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

    // ==================== Shift assignments ====================

    @PostMapping("/employees/{employeeId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create shift assignment", description = "Assign a shift to an employee for a validity window. Auto-closes any prior open assignment.")
    public ResponseEntity<ApiResponse<ShiftAssignmentResponse>> createAssignment(
            @PathVariable Long employeeId,
            @Valid @RequestBody ShiftAssignmentRequest request) {
        ShiftAssignmentResponse response = shiftService.createAssignment(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift assignment created successfully", response));
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update shift assignment", description = "Update an existing shift assignment")
    public ResponseEntity<ApiResponse<ShiftAssignmentResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody ShiftAssignmentRequest request) {
        ShiftAssignmentResponse response = shiftService.updateAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Shift assignment updated successfully", response));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Delete shift assignment", description = "Remove a shift assignment")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        shiftService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Shift assignment deleted successfully", null));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Get shift assignment by id")
    public ResponseEntity<ApiResponse<ShiftAssignmentResponse>> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getAssignmentById(id)));
    }

    @GetMapping("/employees/{employeeId}/assignments")
    @Operation(summary = "List shift assignments for employee", description = "Full assignment history for an employee, newest first")
    public ResponseEntity<ApiResponse<List<ShiftAssignmentResponse>>> listAssignmentsForEmployee(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.listAssignmentsForEmployee(employeeId)));
    }

    @GetMapping("/employees/{employeeId}/assignments/current")
    @Operation(summary = "Get current shift assignment for employee")
    public ResponseEntity<ApiResponse<ShiftAssignmentResponse>> getCurrentAssignmentForEmployee(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getCurrentAssignmentForEmployee(employeeId)));
    }

    @GetMapping("/{shiftTypeId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "List assignments for shift type")
    public ResponseEntity<ApiResponse<List<ShiftAssignmentResponse>>> listAssignmentsForShiftType(
            @PathVariable Long shiftTypeId) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.listAssignmentsForShiftType(shiftTypeId)));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Quick-assign shift (legacy)", description = "Assign a shift starting today, open-ended. Prefer POST /shifts/employees/{employeeId}/assignments.")
    public ResponseEntity<ApiResponse<Void>> assignShift(
            @RequestParam Long employeeId,
            @RequestParam Long shiftTypeId) {
        shiftService.assignShift(employeeId, shiftTypeId);
        return ResponseEntity.ok(ApiResponse.success("Shift assigned successfully", null));
    }
}
