package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentResponse;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;

import java.util.List;

public interface ShiftService {

    // ---------- Shift type CRUD ----------

    ShiftTypeResponse createShiftType(ShiftTypeRequest request);

    ShiftTypeResponse updateShiftType(Long id, ShiftTypeRequest request);

    void deleteShiftType(Long id);

    ShiftTypeResponse getShiftTypeById(Long id);

    ShiftTypeResponse getShiftTypeByCode(String code);

    List<ShiftTypeResponse> getAllShiftTypes();

    // ---------- Shift assignment CRUD ----------

    /**
     * Create a new assignment for an employee. If the employee has any
     * existing open-ended assignment, that row is auto-closed by setting
     * its effective_to to (new.effectiveFrom - 1 day). The employee's
     * denormalized shift_type pointer is refreshed to the current shift.
     */
    ShiftAssignmentResponse createAssignment(Long employeeId, ShiftAssignmentRequest request);

    ShiftAssignmentResponse updateAssignment(Long id, ShiftAssignmentRequest request);

    void deleteAssignment(Long id);

    ShiftAssignmentResponse getAssignmentById(Long id);

    List<ShiftAssignmentResponse> listAssignmentsForEmployee(Long employeeId);

    List<ShiftAssignmentResponse> listAssignmentsForShiftType(Long shiftTypeId);

    /**
     * Return the employee's current assignment, or null if they have none.
     */
    ShiftAssignmentResponse getCurrentAssignmentForEmployee(Long employeeId);

    // ---------- Legacy quick-assign (kept for compatibility) ----------

    /**
     * Quick-assign: create a new open-ended assignment starting today.
     * Equivalent to {@link #createAssignment(Long, ShiftAssignmentRequest)}
     * with effectiveFrom=today, effectiveTo=null.
     */
    void assignShift(Long employeeId, Long shiftTypeId);
}
