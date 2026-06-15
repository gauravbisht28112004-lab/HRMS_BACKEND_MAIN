package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeCreateResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.EmployeeMapper;
import com.financebuddha.finbud.hrms.repository.DepartmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.ShiftTypeRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.EmployeeService;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftTypeRepository shiftTypeRepository;
    private final EmployeeMapper employeeMapper;

    // ------------------------------------------------------------------
    // Auth / user-provisioning collaborators. Injected so that the
    // create-employee flow can auto-provision a matching User row (the
    // "nd33454 can't login" bug fix — a manually-created employee used
    // to have no corresponding login row, so /login always failed).
    // ------------------------------------------------------------------
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfig;

    /** Fallback when {@code system_config.auth.default_password} is unset. Kept in sync with V7 Flyway. */
    private static final String FALLBACK_DEFAULT_PASSWORD = "finbud@123";

    @Override
    @Transactional
    public EmployeeCreateResponse createEmployee(EmployeeRequest request) {
        log.info("Creating new employee with email: {}", request.getEmail());

        // Uniqueness check only runs when the caller actually supplied an
        // email. The Excel master data legitimately has rows without an
        // email, and existsByEmail(null) would either match arbitrarily or
        // fall through to a NullPointerException depending on the driver.
        String requestEmail = request.getEmail();
        if (requestEmail != null && !requestEmail.isBlank()
                && employeeRepository.existsByEmail(requestEmail)) {
            throw new DuplicateResourceException("Employee", "email", requestEmail);
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

        EmployeeResponse employeeResponse = employeeMapper.toResponse(savedEmployee);

        // ------------------------------------------------------------------
        // Auto-provision a User account so the new hire can log in. The
        // policy (asked-and-answered with the HR admin):
        //   - Username: employee.loginUsername if set, otherwise employeeId.toLowerCase()
        //   - Password: system_config.auth.default_password (currently "finbud@123" per V7)
        //   - Role:    ROLE_EMPLOYEE (role upgrades go through the admin-user surface)
        //   - passwordChangedAt=null → LoginResponse.mustChangePassword=true on first login
        // ------------------------------------------------------------------
        UserProvisioningResult provisioning = autoProvisionUser(savedEmployee);

        return EmployeeCreateResponse.builder()
                .employee(employeeResponse)
                .userProvisioned(provisioning.provisioned())
                .generatedUsername(provisioning.username())
                .generatedTemporaryPassword(provisioning.temporaryPassword())
                .provisioningSkippedReason(provisioning.skippedReason())
                .build();
    }

    /**
     * Creates a User row for the newly saved employee. Kept non-fatal on skip
     * paths (e.g. username collision after a retry, or User already exists)
     * — the employee row has already been committed and should not be lost
     * just because login provisioning hit an edge case.
     */
    private UserProvisioningResult autoProvisionUser(Employee savedEmployee) {
        // Employee might already have a user row if the flow is retried after
        // a partial failure. Treat that as a successful no-op so the caller
        // doesn't see a misleading "couldn't provision" banner.
        if (userRepository.existsByEmployeeId(savedEmployee.getId())) {
            log.info("User already exists for employeeId={} — skipping auto-provision",
                    savedEmployee.getEmployeeId());
            return UserProvisioningResult.skipped("User already exists for this employee");
        }

        // Username is always the employee ID lowercased (e.g. ND260001 → nd260001)
        // so the two are guaranteed to stay in sync. Any loginUsername supplied
        // in the create request is ignored here — it will be overwritten below.
        String username = savedEmployee.getEmployeeId().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            log.warn("Username '{}' already taken — skipping auto-provision for employeeId={}. "
                            + "Admin must provision this user manually via /api/admin/users.",
                    username, savedEmployee.getEmployeeId());
            return UserProvisioningResult.skipped("Username '" + username + "' is already taken");
        }

        String rawPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, FALLBACK_DEFAULT_PASSWORD);

        Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role", "name", RoleType.ROLE_EMPLOYEE.name()));
        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);

        User user = User.builder()
                .employee(savedEmployee)
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(Boolean.TRUE)
                // passwordChangedAt intentionally left null — the login flow
                // reads this as mustChangePassword=true and forces a rotation
                // before the session is considered fully authenticated.
                .roles(roles)
                .build();

        userRepository.save(user);

        // Keep employee.loginUsername in sync with the provisioned username
        // so both columns always reflect the same value.
        savedEmployee.setLoginUsername(username);
        employeeRepository.save(savedEmployee);
        log.info("Auto-provisioned user '{}' for employeeId={} with ROLE_EMPLOYEE",
                username, savedEmployee.getEmployeeId());

        return UserProvisioningResult.success(username, rawPassword);
    }

    /**
     * Internal carrier for the outcome of {@link #autoProvisionUser(Employee)}.
     * Keeps {@code createEmployee} readable without needing four out-params.
     */
    private record UserProvisioningResult(
            boolean provisioned,
            String username,
            String temporaryPassword,
            String skippedReason) {

        static UserProvisioningResult success(String username, String temporaryPassword) {
            return new UserProvisioningResult(true, username, temporaryPassword, null);
        }

        static UserProvisioningResult skipped(String reason) {
            return new UserProvisioningResult(false, null, null, reason);
        }
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        log.info("Updating employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // Check email uniqueness only when the email actually changed AND a
        // new email was supplied. Many imported rows have no email at all
        // (Excel master doesn't always carry one), so both sides can be
        // null — {@link Objects#equals} handles that, and the existsByEmail
        // lookup is skipped when the new value is null/blank.
        String oldEmail = employee.getEmail();
        String newEmail = request.getEmail();
        boolean emailChanged = !Objects.equals(oldEmail, newEmail);
        boolean newEmailHasValue = newEmail != null && !newEmail.isBlank();
        if (emailChanged && newEmailHasValue && employeeRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Employee", "email", newEmail);
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

        // Soft delete: mark employee as terminated
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);

        // Disable the linked User account so the person can no longer log in.
        // Fetch via repository directly — avoids relying on the lazy-loaded
        // inverse @OneToOne proxy on employee.getUser() which can be null
        // even when a user row exists, causing a silent transaction rollback.
        userRepository.findByEmployeeId(id).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
            log.info("User account disabled for terminated employee: {}", employee.getEmployeeId());
        });

        log.info("Employee soft-deleted (TERMINATED): {}", employee.getEmployeeId());
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
        // Exclude TERMINATED employees — they have been soft-deleted and should
        // not appear in the default employee list.
        Page<Employee> employeePage = employeeRepository.findByStatusNot(EmployeeStatus.TERMINATED, pageable);

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
        return "ND" + year + String.format("%04d", count);
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
