package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.HourlyUpdate;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.HourlyUpdateRepository;
import com.financebuddha.finbud.hrms.service.HourlyUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HourlyUpdateServiceImpl implements HourlyUpdateService {

    private final HourlyUpdateRepository hourlyUpdateRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public HourlyUpdateResponse upsert(Long employeeId, HourlyUpdateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // Upsert pattern — same (employee, date, slot) updates the existing
        // row in place, otherwise insert. The DB also enforces this with a
        // UNIQUE constraint as a belt-and-braces guard against races.
        HourlyUpdate row = hourlyUpdateRepository
                .findByEmployeeIdAndWorkDateAndHourSlot(employeeId, request.getWorkDate(), request.getHourSlot())
                .orElseGet(() -> HourlyUpdate.builder()
                        .employee(employee)
                        .workDate(request.getWorkDate())
                        .hourSlot(request.getHourSlot())
                        .build());

        row.setCallsDone(request.getCallsDone());
        row.setOtpsAchieved(request.getOtpsAchieved());
        row.setInterestedCustomers(request.getInterestedCustomers());
        row.setNotes(request.getNotes());

        return toResponse(hourlyUpdateRepository.save(row));
    }

    @Override
    @Transactional
    public void delete(Long updateId, Long callerEmployeeId) {
        HourlyUpdate row = hourlyUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("HourlyUpdate", "id", updateId));
        if (!row.getEmployee().getId().equals(callerEmployeeId)) {
            throw new ForbiddenException("You can only delete your own hourly updates");
        }
        hourlyUpdateRepository.delete(row);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyUpdateResponse> listMineForDate(Long employeeId, LocalDate workDate) {
        return hourlyUpdateRepository
                .findByEmployeeIdAndWorkDateOrderByHourSlotAsc(employeeId, workDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyUpdateResponse> listMineForRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return hourlyUpdateRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDescHourSlotAsc(employeeId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyUpdateResponse> listTeamForDate(Long managerId, LocalDate workDate) {
        return hourlyUpdateRepository
                .findByManagerIdAndWorkDate(managerId, workDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private HourlyUpdateResponse toResponse(HourlyUpdate h) {
        Employee employee = h.getEmployee();
        return HourlyUpdateResponse.builder()
                .id(h.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeCode(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getFullName() : null)
                .workDate(h.getWorkDate())
                .hourSlot(h.getHourSlot())
                .callsDone(h.getCallsDone())
                .otpsAchieved(h.getOtpsAchieved())
                .interestedCustomers(h.getInterestedCustomers())
                .notes(h.getNotes())
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}
