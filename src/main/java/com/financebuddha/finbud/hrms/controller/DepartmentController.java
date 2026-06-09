package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.department.DepartmentRequest;
import com.financebuddha.finbud.hrms.dto.department.DepartmentResponse;
import com.financebuddha.finbud.hrms.service.DepartmentService;
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

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create department", description = "Create a new department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Department created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update department", description = "Update an existing department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Delete department", description = "Delete a department")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Get department details by ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get department by code", description = "Get department by department code")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentByCode(@PathVariable String code) {
        DepartmentResponse response = departmentService.getDepartmentByCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all departments", description = "Get paginated list of all departments")
    public ResponseEntity<ApiResponse<PagedResponse<DepartmentResponse>>> getAllDepartments(
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<DepartmentResponse> response = departmentService.getAllDepartments(paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search departments", description = "Search departments by name")
    public ResponseEntity<ApiResponse<PagedResponse<DepartmentResponse>>> searchDepartments(
            @RequestParam String query,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<DepartmentResponse> response = departmentService.searchDepartments(query, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{departmentId}/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Assign manager", description = "Assign a manager to department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> assignManager(
            @PathVariable Long departmentId,
            @PathVariable Long managerId) {
        DepartmentResponse response = departmentService.assignManager(departmentId, managerId);
        return ResponseEntity.ok(ApiResponse.success("Manager assigned successfully", response));
    }

    @GetMapping("/{departmentId}/employee-count")
    @Operation(summary = "Get employee count", description = "Get count of employees in department")
    public ResponseEntity<ApiResponse<Long>> countEmployeesByDepartment(@PathVariable Long departmentId) {
        long count = departmentService.countEmployeesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
