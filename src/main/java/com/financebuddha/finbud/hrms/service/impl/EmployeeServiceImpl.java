package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.EmployeeMapper;
import com.financebuddha.finbud.hrms.repository.DepartmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.ShiftTypeRepository;
import com.financebuddha.finbud.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftTypeRepository shiftTypeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating new employee with email: {}", request.getEmail());

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", request.getEmail());
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setEmployeeId(generateEmployeeId());

        // Set department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        // Set manager if provided
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "managerId", request.getManagerId()));
            employee.setManager(manager);
        }

        // Set shift if provided
        if (request.getShiftTypeId() != null) {
            ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", request.getShiftTypeId()));
            employee.setShiftType(shiftType);
        }

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with ID: {}", savedEmployee.getId());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        log.info("Updating employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // Check email uniqueness if changed
        if (!employee.getEmail().equals(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", request.getEmail());
        }

        employeeMapper.updateEntityFromRequest(request, employee);

        // Update department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        // Update manager if provided
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "managerId", request.getManagerId()));
            employee.setManager(manager);
        }

        // Update shift if provided
        if (request.getShiftTypeId() != null) {
            ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", request.getShiftTypeId()));
            employee.setShiftType(shiftType);
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", updatedEmployee.getId());

        return employeeMapper.toResponse(updatedEmployee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // Soft delete by marking as terminated
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);

        log.info("Employee marked as terminated: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return employeeMapper.toDetailResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByEmployeeId(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", employeeId));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getAllEmployees(PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findAll(pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployeesByDepartment(Long departmentId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findByDepartmentId(departmentId, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployeesByManager(Long managerId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findByManagerId(managerId, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployeesByShift(Long shiftTypeId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findByShiftTypeId(shiftTypeId, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployeesByStatus(EmployeeStatus status, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findByStatus(status, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(String search, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.searchEmployees(search, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployeesByFilters(Long departmentId, EmployeeStatus status, Long managerId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Employee> employeePage = employeeRepository.findByFilters(departmentId, status, managerId, pageable);

        return PagedResponse.of(
                employeeMapper.toResponseList(employeePage.getContent()),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getActiveSubordinates(Long managerId) {
        List<Employee> subordinates = employeeRepository.findActiveSubordinates(managerId);
        return employeeMapper.toResponseList(subordinates);
    }

    @Override
    public String generateEmployeeId() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yy"));
        long count = employeeRepository.count() + 1;
        return "FBD" + year + String.format("%04d", count);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEmployeeCountByStatus(EmployeeStatus status) {
        return employeeRepository.countByStatus(status);
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
