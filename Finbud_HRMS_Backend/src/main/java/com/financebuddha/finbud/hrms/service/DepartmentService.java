package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.department.DepartmentRequest;
import com.financebuddha.finbud.hrms.dto.department.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    void deleteDepartment(Long id);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse getDepartmentByCode(String code);

    PagedResponse<DepartmentResponse> getAllDepartments(PaginationRequest paginationRequest);

    PagedResponse<DepartmentResponse> searchDepartments(String search, PaginationRequest paginationRequest);

    DepartmentResponse assignManager(Long departmentId, Long managerId);

    long countEmployeesByDepartment(Long departmentId);
}
