package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;

import java.util.List;

public interface ShiftService {

    ShiftTypeResponse createShiftType(ShiftTypeRequest request);

    ShiftTypeResponse updateShiftType(Long id, ShiftTypeRequest request);

    void deleteShiftType(Long id);

    ShiftTypeResponse getShiftTypeById(Long id);

    ShiftTypeResponse getShiftTypeByCode(String code);

    List<ShiftTypeResponse> getAllShiftTypes();

    void assignShift(Long employeeId, Long shiftTypeId);
}
