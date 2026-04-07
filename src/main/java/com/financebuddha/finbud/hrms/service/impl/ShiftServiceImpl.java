package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.ShiftTypeMapper;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.ShiftTypeRepository;
import com.financebuddha.finbud.hrms.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftTypeRepository shiftTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftTypeMapper shiftTypeMapper;

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

    @Override
    @Transactional
    public void assignShift(Long employeeId, Long shiftTypeId) {
        log.info("Assigning shift {} to employee {}", shiftTypeId, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        ShiftType shiftType = shiftTypeRepository.findById(shiftTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftType", "id", shiftTypeId));

        employee.setShiftType(shiftType);
        employeeRepository.save(employee);
    }
}
