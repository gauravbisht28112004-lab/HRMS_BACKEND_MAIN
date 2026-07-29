package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureRequest;
import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureResponse;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
@Tag(name = "Salary Management", description = "Salary structure management APIs")
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping("/{employeeId}")
    @Operation(summary = "Get employee salary structure", description = "Get salary structure for a specific employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER') or @userPrincipalService.isEmployee(#employeeId)")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getSalaryStructure(
            @PathVariable Long employeeId) {
        SalaryStructureResponse response = salaryService.getSalaryStructure(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Salary structure retrieved successfully", response));
    }

    @PostMapping("/{employeeId}")
    @Operation(summary = "Create salary structure", description = "Create salary structure for an employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> createSalaryStructure(
            @PathVariable Long employeeId,
            @Valid @RequestBody SalaryStructureRequest request) {
        SalaryStructureResponse response = salaryService.createSalaryStructure(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Salary structure created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update salary structure", description = "Update existing salary structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> updateSalaryStructure(
            @PathVariable Long id,
            @Valid @RequestBody SalaryStructureRequest request) {
        SalaryStructureResponse response = salaryService.updateSalaryStructure(id, request);
        return ResponseEntity.ok(ApiResponse.success("Salary structure updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate salary structure", description = "Deactivate a salary structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<Void>> deactivateSalaryStructure(@PathVariable Long id) {
        salaryService.deactivateSalaryStructure(id);
        return ResponseEntity.ok(ApiResponse.success("Salary structure deactivated successfully", null));
    }

    @GetMapping
    @Operation(summary = "Get all salary structures", description = "Get all salary structures")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<SalaryStructureResponse>>> getAllSalaryStructures() {
        List<SalaryStructureResponse> responses = salaryService.getAllSalaryStructures();
        return ResponseEntity.ok(ApiResponse.success("Salary structures retrieved successfully", responses));
    }
}
