package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentResponse;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.ShiftAssignment;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.ShiftAssignmentMapper;
import com.financebuddha.finbud.hrms.mapper.ShiftTypeMapper;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.ShiftAssignmentRepository;
import com.financebuddha.finbud.hrms.repository.ShiftTypeRepository;
import com.financebuddha.finbud.hrms.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftTypeRepository shiftTypeRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftTypeMapper shiftTypeMapper;
    private final ShiftAssignmentMapper shiftAssignmentMapper;

    // =================================================================
    // Shift type CRUD
    // =================================================================

    @Override
    @Transactional
    public ShiftTypeResponse createShiftType(ShiftTypeRequest request) {
        log.info("Creating shift type: {}", request.getName());

        if (shiftTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("ShiftType", "code", request.getCode());
        }

        ShiftType shiftType = shiftTypeMapper.toEntity(request);
        ShiftType savedShift = shiftTypeRepository.save(shiftType);

        return shiftTypeMapper.toResponse(savedShift);
    }

    @Override
    @Transactional
    public ShiftTypeResponse updateShiftType(Long id, ShiftTypeRequest request) {
        log.info("Updating shift type: {}", id);

        ShiftType shiftType = shiftTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", id));

        if (!shiftType.getCode().equals(request.getCode()) && shiftTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("ShiftType", "code", request.getCode());
        }

        shiftTypeMapper.updateEntityFromRequest(request, shiftType);
        ShiftType updatedShift = shiftTypeRepository.save(shiftType);

        return shiftTypeMapper.toResponse(updatedShift);
    }

    @Override
    @Transactional
    public void deleteShiftType(Long id) {
        log.info("Deleting shift type: {}", id);

        ShiftType shiftType = shiftTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", id));

        if (shiftAssignmentRepository.existsByShiftTypeId(id)) {
            throw new BadRequestException(
                    "Shift type is assigned to one or more employees and cannot be deleted. " +
                    "Reassign employees first.");
        }

        shiftTypeRepository.delete(shiftType);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftTypeResponse getShiftTypeById(Long id) {
        ShiftType shiftType = shiftTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", id));
        return shiftTypeMapper.toResponse(shiftType);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftTypeResponse getShiftTypeByCode(String code) {
        ShiftType shiftType = shiftTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "code", code));
        return shiftTypeMapper.toResponse(shiftType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftTypeResponse> getAllShiftTypes() {
        List<ShiftType> shifts = shiftTypeRepository.findAll();
        return shiftTypeMapper.toResponseList(shifts);
    }

    // =================================================================
    // Shift assignment CRUD
    // =================================================================

    @Override
    @Transactional
    public ShiftAssignmentResponse createAssignment(Long employeeId, ShiftAssignmentRequest request) {
        log.info("Creating shift assignment for employee {} shiftType {} from {} to {}",
                employeeId, request.getShiftTypeId(), request.getEffectiveFrom(), request.getEffectiveTo());

        validateWindow(request.getEffectiveFrom(), request.getEffectiveTo());

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", request.getShiftTypeId()));

        autoCloseOpenAssignments(employeeId, request.getEffectiveFrom());

        ShiftAssignment assignment = ShiftAssignment.builder()
                .employee(employee)
                .shiftType(shiftType)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);

        syncEmployeeDenormalizedShiftType(employee);

        return shiftAssignmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ShiftAssignmentResponse updateAssignment(Long id, ShiftAssignmentRequest request) {
        log.info("Updating shift assignment {}", id);

        validateWindow(request.getEffectiveFrom(), request.getEffectiveTo());

        ShiftAssignment assignment = shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", "id", id));

        ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", request.getShiftTypeId()));

        assignment.setShiftType(shiftType);
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setEffectiveTo(request.getEffectiveTo());

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);

        syncEmployeeDenormalizedShiftType(assignment.getEmployee());

        return shiftAssignmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id) {
        log.info("Deleting shift assignment {}", id);

        ShiftAssignment assignment = shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", "id", id));

        Employee employee = assignment.getEmployee();
        shiftAssignmentRepository.delete(assignment);

        syncEmployeeDenormalizedShiftType(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftAssignmentResponse getAssignmentById(Long id) {
        ShiftAssignment assignment = shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", "id", id));
        return shiftAssignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponse> listAssignmentsForEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }
        return shiftAssignmentMapper.toResponseList(
                shiftAssignmentRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponse> listAssignmentsForShiftType(Long shiftTypeId) {
        if (!shiftTypeRepository.existsById(shiftTypeId)) {
            throw new ResourceNotFoundException("ShiftType", "id", shiftTypeId);
        }
        return shiftAssignmentMapper.toResponseList(
                shiftAssignmentRepository.findByShiftTypeIdOrderByEffectiveFromDesc(shiftTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftAssignmentResponse getCurrentAssignmentForEmployee(Long employeeId) {
        return shiftAssignmentRepository
                .findActiveAssignmentForEmployee(employeeId, LocalDate.now())
                .map(shiftAssignmentMapper::toResponse)
                .orElse(null);
    }

    // =================================================================
    // Legacy quick-assign (delegates to createAssignment)
    // =================================================================

    @Override
    @Transactional
    public void assignShift(Long employeeId, Long shiftTypeId) {
        log.info("Quick-assigning shift {} to employee {} (open-ended, starting today)", shiftTypeId, employeeId);

        ShiftAssignmentRequest request = new ShiftAssignmentRequest();
        request.setShiftTypeId(shiftTypeId);
        request.setEffectiveFrom(LocalDate.now());
        request.setEffectiveTo(null);

        createAssignment(employeeId, request);
    }

    // =================================================================
    // Helpers
    // =================================================================

    private void validateWindow(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new BadRequestException("Effective-from date is required");
        }
        if (to != null && to.isBefore(from)) {
            throw new BadRequestException("Effective-to date cannot be before effective-from date");
        }
    }

    /**
     * Close any currently-open assignments for the employee by setting
     * effective_to to (newStart - 1 day). If the new assignment starts on
     * the same day as an existing open one, we close the old one at
     * (newStart - 1) so the windows don't overlap; the DB CHECK enforces
     * effective_to >= effective_from so we guard against producing a
     * negative-length window by instead deleting same-day zero-length
     * leftovers — in practice this only happens if newStart == old.from,
     * and the old row hasn't been used anywhere yet.
     */
    private void autoCloseOpenAssignments(Long employeeId, LocalDate newStart) {
        List<ShiftAssignment> openRows = shiftAssignmentRepository.findOpenAssignmentsForEmployee(employeeId);
        if (openRows.isEmpty()) return;

        LocalDate closeOn = newStart.minusDays(1);
        for (ShiftAssignment open : openRows) {
            if (closeOn.isBefore(open.getEffectiveFrom())) {
                // new start is on/before the old start — old row would collapse
                // to a negative window. Safer to drop the stale row than to
                // pretend it was historically meaningful.
                log.warn("Deleting shift_assignment {} because new assignment starts on/before its effectiveFrom ({})",
                        open.getId(), open.getEffectiveFrom());
                shiftAssignmentRepository.delete(open);
            } else {
                open.setEffectiveTo(closeOn);
                shiftAssignmentRepository.save(open);
            }
        }
    }

    /**
     * Refresh Employee.shiftType from the current (today-active) assignment.
     * If the employee has no current assignment, null out the pointer so
     * the UI doesn't lie. Fetches the employee fresh to avoid stale state
     * if the caller passed in a detached proxy.
     */
    private void syncEmployeeDenormalizedShiftType(Employee employee) {
        if (employee == null) return;

        Long employeeId = employee.getId();
        Employee managed = employeeRepository.findById(employeeId)
                .orElse(null);
        if (managed == null) return;

        ShiftType current = shiftAssignmentRepository
                .findActiveAssignmentForEmployee(employeeId, LocalDate.now())
                .map(ShiftAssignment::getShiftType)
                .orElse(null);

        managed.setShiftType(current);
        employeeRepository.save(managed);
    }
}
