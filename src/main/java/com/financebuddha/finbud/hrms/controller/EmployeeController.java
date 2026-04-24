package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeCreateResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.service.EmployeeService;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management APIs")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create employee",
               description = "Create a new employee and auto-provision a login account. "
                           + "The response includes the generated username and a one-time "
                           + "plaintext temporary password which the UI must surface once "
                           + "and never display again.")
    public ResponseEntity<ApiResponse<EmployeeCreateResponse>> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeCreateResponse response = employeeService.createEmployee(request);
        String message = Boolean.TRUE.equals(response.getUserProvisioned())
                ? "Employee created and login provisioned successfully"
                : "Employee created successfully (login not provisioned: "
                        + response.getProvisioningSkippedReason() + ")";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update employee", description = "Update an existing employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Delete employee", description = "Delete an employee (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Get employee details by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Long id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get employee details", description = "Get detailed employee information")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployeeDetails(@PathVariable Long id) {
        EmployeeDetailResponse response = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee-id/{employeeId}")
    @Operation(summary = "Get employee by employee ID", description = "Get employee by employee code")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByEmployeeId(@PathVariable String employeeId) {
        EmployeeResponse response = employeeService.getEmployeeByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all employees", description = "Get paginated list of all employees")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getAllEmployees(
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getAllEmployees(paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get employees by department", description = "Get employees by department ID")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getEmployeesByDepartment(
            @PathVariable Long departmentId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getEmployeesByDepartment(departmentId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Get employees by manager", description = "Get subordinates of a manager")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getEmployeesByManager(
            @PathVariable Long managerId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getEmployeesByManager(managerId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/shift/{shiftTypeId}")
    @Operation(summary = "Get employees by shift", description = "Get employees assigned to a shift")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getEmployeesByShift(
            @PathVariable Long shiftTypeId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getEmployeesByShift(shiftTypeId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get employees by status", description = "Get employees filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getEmployeesByStatus(
            @PathVariable EmployeeStatus status,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getEmployeesByStatus(status, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search employees", description = "Search employees by name, email, or employee ID")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> searchEmployees(
            @RequestParam String query,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.searchEmployees(query, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/filters")
    @Operation(summary = "Filter employees", description = "Filter employees by department, status, and manager")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getEmployeesByFilters(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Long managerId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<EmployeeResponse> response = employeeService.getEmployeesByFilters(
                departmentId, status, managerId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager/{managerId}/subordinates")
    @Operation(summary = "Get active subordinates", description = "Get active subordinates of a manager")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getActiveSubordinates(@PathVariable Long managerId) {
        List<EmployeeResponse> response = employeeService.getActiveSubordinates(managerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
