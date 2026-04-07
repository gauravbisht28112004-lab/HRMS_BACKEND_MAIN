package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.entity.Attendance;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.AttendanceMapper;
import com.financebuddha.finbud.hrms.repository.AttendanceRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    @Transactional
    public AttendanceResponse recordPunchIn(PunchRequest request) {
        log.info("Recording punch in for employee: {}", request.getEmployeeId());

        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", request.getEmployeeId()));

        LocalDate today = request.getTimestamp().toLocalDate();

        // Check if already punched in
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElse(null);

        if (attendance == null) {
            attendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(today)
                    .punchIn(request.getTimestamp())
                    .shiftType(employee.getShiftType())
                    .deviceId(request.getDeviceId())
                    .punchInLocation(request.getLocation())
                    .status(AttendanceStatus.PRESENT)
                    .build();
        } else if (attendance.getPunchIn() != null) {
            throw new BadRequestException("Already punched in for today");
        } else {
            attendance.setPunchIn(request.getTimestamp());
            attendance.setDeviceId(request.getDeviceId());
            attendance.setPunchInLocation(request.getLocation());
        }

        // Check for late coming
        checkLateComing(attendance);

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(savedAttendance);
    }

    @Override
    @Transactional
    public AttendanceResponse recordPunchOut(PunchRequest request) {
        log.info("Recording punch out for employee: {}", request.getEmployeeId());

        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", request.getEmployeeId()));

        LocalDate today = request.getTimestamp().toLocalDate();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("No punch in record found for today"));

        if (attendance.getPunchOut() != null) {
            throw new BadRequestException("Already punched out for today");
        }

        attendance.setPunchOut(request.getTimestamp());
        attendance.setPunchOutLocation(request.getLocation());

        // Calculate working hours
        calculateWorkingHours(attendance);

        // Check for early leave
        checkEarlyLeave(attendance);

        // Check for overtime
        checkOvertime(attendance);

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(savedAttendance);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "date", date));
        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> getAttendanceByEmployee(Long employeeId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Attendance> attendancePage = attendanceRepository.findByEmployeeId(employeeId, pageable);

        return PagedResponse.of(
                attendanceMapper.toResponseList(attendancePage.getContent()),
                attendancePage.getNumber(),
                attendancePage.getSize(),
                attendancePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate);
        return attendanceMapper.toResponseList(attendances);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLateComersByDate(LocalDate date) {
        List<Attendance> lateAttendances = attendanceRepository.findLateComersByDate(date);
        return attendanceMapper.toResponseList(lateAttendances);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAbsentByDate(LocalDate date) {
        List<Attendance> absentAttendances = attendanceRepository.findAbsentByDate(date);
        return attendanceMapper.toResponseList(absentAttendances);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getOvertimeByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Attendance> overtimeAttendances = attendanceRepository.findOvertimeByDateRange(startDate, endDate);
        return attendanceMapper.toResponseList(overtimeAttendances);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Long employeeId, int month, int year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        Long presentDays = attendanceRepository.countPresentDaysByEmployeeAndDateRange(employeeId, startOfMonth, endOfMonth);
        Long lateCount = attendanceRepository.countLateDaysByEmployeeAndDateRange(employeeId, startOfMonth, endOfMonth);
        Long halfDays = attendanceRepository.countHalfDaysByEmployeeAndDateRange(employeeId, startOfMonth, endOfMonth);
        Double overtimeHours = attendanceRepository.sumOvertimeHoursByEmployeeAndDateRange(employeeId, startOfMonth, endOfMonth);

        int totalWorkingDays = calculateWorkingDays(startOfMonth, endOfMonth);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        return AttendanceSummaryResponse.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFullName())
                .month(startOfMonth.getMonth().name())
                .year(year)
                .totalDays(totalWorkingDays)
                .presentDays(presentDays != null ? presentDays.intValue() : 0)
                .absentDays(totalWorkingDays - (presentDays != null ? presentDays.intValue() : 0))
                .halfDays(halfDays != null ? halfDays.intValue() : 0)
                .lateCount(lateCount != null ? lateCount.intValue() : 0)
                .totalOvertimeHours(BigDecimal.valueOf(overtimeHours != null ? overtimeHours : 0))
                .build();
    }

    @Override
    @Transactional
    public void processDailyAttendance(LocalDate date) {
        log.info("Processing daily attendance for date: {}", date);
        // This method would be called by a scheduled job to process attendance
        // Mark absent employees, etc.
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPunchedInToday(Long employeeId) {
        return attendanceRepository.existsByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPunchedOutToday(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now())
                .map(a -> a.getPunchOut() != null)
                .orElse(false);
    }

    private void checkLateComing(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null) return;

        LocalDateTime expectedStart = LocalDateTime.of(attendance.getAttendanceDate(), shift.getStartTime());
        LocalDateTime gracePeriodEnd = expectedStart.plusMinutes(shift.getGracePeriodMinutes());

        if (attendance.getPunchIn().isAfter(gracePeriodEnd)) {
            attendance.setIsLate(true);
            attendance.setLateMinutes((int) Duration.between(expectedStart, attendance.getPunchIn()).toMinutes());
        }
    }

    private void calculateWorkingHours(Attendance attendance) {
        if (attendance.getPunchIn() == null || attendance.getPunchOut() == null) return;

        Duration duration = Duration.between(attendance.getPunchIn(), attendance.getPunchOut());
        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes() / 60.0);
        BigDecimal breakHours = attendance.getBreakHours() != null ? attendance.getBreakHours() : BigDecimal.ONE;

        attendance.setWorkingHours(totalHours.subtract(breakHours).max(BigDecimal.ZERO));

        // Check for half day
        if (attendance.getWorkingHours().compareTo(new BigDecimal("4")) < 0) {
            attendance.setIsHalfDay(true);
        }
    }

    private void checkEarlyLeave(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null || attendance.getPunchOut() == null) return;

        LocalDateTime expectedEnd = LocalDateTime.of(attendance.getAttendanceDate(), shift.getEndTime());

        if (attendance.getPunchOut().isBefore(expectedEnd)) {
            attendance.setIsEarlyLeave(true);
            attendance.setEarlyLeaveMinutes((int) Duration.between(attendance.getPunchOut(), expectedEnd).toMinutes());
        }
    }

    private void checkOvertime(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null || attendance.getWorkingHours() == null) return;

        BigDecimal threshold = BigDecimal.valueOf(shift.getOvertimeThresholdHours());
        if (attendance.getWorkingHours().compareTo(threshold) > 0) {
            attendance.setIsOvertime(true);
            attendance.setOvertimeHours(attendance.getWorkingHours().subtract(threshold));
        }
    }

    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int workingDays = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() <= 5) {
                workingDays++;
            }
            date = date.plusDays(1);
        }
        return workingDays;
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
