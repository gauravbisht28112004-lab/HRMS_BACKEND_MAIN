package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeDetailResponse getEmployeeDetail(Long id);

    EmployeeResponse getEmployeeByEmployeeId(String employeeId);

    PagedResponse<EmployeeResponse> getAllEmployees(PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> getEmployeesByDepartment(Long departmentId, PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> getEmployeesByManager(Long managerId, PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> getEmployeesByShift(Long shiftTypeId, PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> getEmployeesByStatus(EmployeeStatus status, PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> searchEmployees(String search, PaginationRequest paginationRequest);

    PagedResponse<EmployeeResponse> getEmployeesByFilters(Long departmentId, EmployeeStatus status, Long managerId, PaginationRequest paginationRequest);

    List<EmployeeResponse> getActiveSubordinates(Long managerId);

    String generateEmployeeId();

    long getEmployeeCountByStatus(EmployeeStatus status);
}
