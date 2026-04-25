package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceApprovalRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceManualEntryRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.entity.Attendance;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.entity.OfficeLocation;
import com.financebuddha.finbud.hrms.entity.ShiftAssignment;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import com.financebuddha.finbud.hrms.enums.AttendanceApprovalStatus;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.AttendanceMapper;
import com.financebuddha.finbud.hrms.repository.AttendanceRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.repository.PublicHolidayRepository;
import com.financebuddha.finbud.hrms.repository.ShiftAssignmentRepository;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HR = "ROLE_HR";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    // Default shift end when the employee has no shift. Used for the
    // auto-close scheduler threshold — pessimistic so we don't flag too eagerly.
    private static final LocalTime FALLBACK_SHIFT_END = LocalTime.of(19, 0);

    // Earth radius used by the Haversine geofence check (in metres).
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PublicHolidayRepository publicHolidayRepository;

    // =====================================================================
    // Portal punch flow
    // =====================================================================

    @Override
    @Transactional
    public AttendanceResponse recordPunchIn(PunchRequest request, UserPrincipal principal) {
        Employee employee = resolveCaller(principal);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Block weekly-off and public holiday punches so we don't collect junk data.
        ShiftType effectiveShift = resolveShiftForDate(employee, today);
        if (isWeeklyOff(effectiveShift, today)) {
            throw new BadRequestException("Today is your weekly off. Contact HR if you need to work a weekly-off day.");
        }
        if (publicHolidayRepository.existsByHolidayDate(today)) {
            throw new BadRequestException("Today is a public holiday. Contact HR if you need to work on a holiday.");
        }

        // Optional geofence enforcement.
        assertInsideGeofence(employee, request);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElse(null);

        if (attendance != null && attendance.getPunchIn() != null) {
            throw new BadRequestException("You have already punched in today.");
        }

        // Admin / HR / Team Leader punches auto-approve — only Employee role
        // punches sit in the approval queue. The TL queue is the only one that
        // matters now: HR/Admin/TL self-punches go straight to APPROVED with
        // the punching user themselves recorded as the approver for audit.
        boolean autoApprove = hasAnyAuthority(principal, ROLE_ADMIN, ROLE_HR, ROLE_MANAGER);
        AttendanceApprovalStatus initialStatus =
                autoApprove ? AttendanceApprovalStatus.APPROVED : AttendanceApprovalStatus.PENDING;

        if (attendance == null) {
            attendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(today)
                    .shiftType(effectiveShift)
                    .approvalStatus(initialStatus)
                    .status(AttendanceStatus.PRESENT)
                    .build();
        } else {
            // Row exists (e.g. auto-absent from previous day rollup was wrong
            // and HR cleared it). Reset approval state for the new punch.
            attendance.setApprovalStatus(initialStatus);
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setIsAutoAbsent(false);
            attendance.setRejectionReason(null);
        }

        if (autoApprove) {
            attendance.setApprovedBy(employee);
            attendance.setApprovedAt(now);
        } else {
            attendance.setApprovedBy(null);
            attendance.setApprovedAt(null);
        }

        attendance.setPunchIn(now);
        attendance.setDeviceId(request.getDeviceId());
        attendance.setPunchInLocation(request.getLocationLabel());
        attendance.setPunchInLatitude(request.getLatitude());
        attendance.setPunchInLongitude(request.getLongitude());
        attendance.setPunchInAccuracyMeters(request.getAccuracyMeters());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            attendance.setNotes(request.getNotes());
        }

        checkLateComing(attendance);
        Attendance saved = attendanceRepository.save(attendance);
        log.info("Punch-in recorded: employee={}, attendance={}", employee.getEmployeeId(), saved.getId());
        return attendanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponse recordPunchOut(PunchRequest request, UserPrincipal principal) {
        Employee employee = resolveCaller(principal);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        assertInsideGeofence(employee, request);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("No punch-in found for today. Punch in first."));

        if (attendance.getPunchIn() == null) {
            throw new BadRequestException("No punch-in found for today. Punch in first.");
        }

        if (attendance.getPunchOut() != null) {
            throw new BadRequestException("You have already punched out today.");
        }

        attendance.setPunchOut(now);
        attendance.setPunchOutLocation(request.getLocationLabel());
        attendance.setPunchOutLatitude(request.getLatitude());
        attendance.setPunchOutLongitude(request.getLongitude());
        attendance.setPunchOutAccuracyMeters(request.getAccuracyMeters());
        // Clear missing-punch flag if a prior scheduler pass had set it.
        attendance.setIsMissingPunch(false);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            attendance.setNotes(request.getNotes());
        }

        calculateWorkingHours(attendance);
        checkEarlyLeave(attendance);
        checkOvertime(attendance);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Punch-out recorded: employee={}, attendance={}", employee.getEmployeeId(), saved.getId());
        return attendanceMapper.toResponse(saved);
    }

    // =====================================================================
    // Approval workflow
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getPendingApprovals(UserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        List<Attendance> pending;
        if (hasAnyAuthority(principal, ROLE_ADMIN, ROLE_HR)) {
            pending = attendanceRepository
                    .findByApprovalStatusOrderByAttendanceDateDescIdDesc(AttendanceApprovalStatus.PENDING);
        } else if (hasAnyAuthority(principal, ROLE_MANAGER)) {
            Employee manager = resolveCaller(principal);
            pending = attendanceRepository.findByStatusAndManager(AttendanceApprovalStatus.PENDING, manager.getId());
        } else {
            throw new ForbiddenException("You do not have permission to view the approval queue.");
        }
        return attendanceMapper.toResponseList(pending);
    }

    @Override
    @Transactional
    public AttendanceResponse reviewAttendance(Long attendanceId,
                                               AttendanceApprovalRequest request,
                                               UserPrincipal principal) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", attendanceId));

        Employee reviewer = resolveCaller(principal);

        // Authorisation: HR/Admin any, Manager only for direct reports.
        if (!hasAnyAuthority(principal, ROLE_ADMIN, ROLE_HR)) {
            if (!hasAnyAuthority(principal, ROLE_MANAGER)
                    || attendance.getEmployee().getManager() == null
                    || !Objects.equals(attendance.getEmployee().getManager().getId(), reviewer.getId())) {
                throw new ForbiddenException("You can only review attendance of your direct reports.");
            }
        }

        if (attendance.getApprovalStatus() != AttendanceApprovalStatus.PENDING) {
            throw new BadRequestException("This attendance row has already been " +
                    attendance.getApprovalStatus().name().toLowerCase() + ".");
        }

        if (Boolean.TRUE.equals(request.getApprove())) {
            attendance.setApprovalStatus(AttendanceApprovalStatus.APPROVED);
            attendance.setApprovedBy(reviewer);
            attendance.setApprovedAt(LocalDateTime.now());
            attendance.setRejectionReason(null);
        } else {
            String reason = request.getRejectionReason();
            if (reason == null || reason.isBlank()) {
                throw new BadRequestException("Rejection reason is required.");
            }
            attendance.setApprovalStatus(AttendanceApprovalStatus.REJECTED);
            attendance.setApprovedBy(reviewer);
            attendance.setApprovedAt(LocalDateTime.now());
            attendance.setRejectionReason(reason.trim());
        }

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponse manualEntry(AttendanceManualEntryRequest request, UserPrincipal principal) {
        if (!hasAnyAuthority(principal, ROLE_ADMIN, ROLE_HR)) {
            throw new ForbiddenException("Manual attendance entry is restricted to HR / Admin.");
        }
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        Employee editor = resolveCaller(principal);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), request.getAttendanceDate())
                .orElseGet(() -> Attendance.builder()
                        .employee(employee)
                        .attendanceDate(request.getAttendanceDate())
                        .shiftType(resolveShiftForDate(employee, request.getAttendanceDate()))
                        .build());

        attendance.setPunchIn(request.getPunchIn());
        attendance.setPunchOut(request.getPunchOut());
        attendance.setStatus(request.getStatus());
        attendance.setNotes(request.getNotes());
        // Clear scheduler flags — we're taking ownership.
        attendance.setIsAutoAbsent(false);
        attendance.setIsMissingPunch(false);
        attendance.setManuallyEditedBy(editor);
        attendance.setManuallyEditedAt(LocalDateTime.now());

        // HR-authored rows are auto-approved.
        attendance.setApprovalStatus(AttendanceApprovalStatus.APPROVED);
        attendance.setApprovedBy(editor);
        attendance.setApprovedAt(LocalDateTime.now());
        attendance.setRejectionReason(null);

        calculateWorkingHours(attendance);
        checkLateComing(attendance);
        checkEarlyLeave(attendance);
        checkOvertime(attendance);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Manual attendance entry: employee={}, date={}, status={} by={}",
                employee.getEmployeeId(), request.getAttendanceDate(), request.getStatus(), editor.getEmployeeId());
        return attendanceMapper.toResponse(saved);
    }

    // =====================================================================
    // Reads
    // =====================================================================

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
        Page<Attendance> page = attendanceRepository.findByEmployeeId(employeeId, pageable);
        return PagedResponse.of(
                attendanceMapper.toResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceMapper.toResponseList(
                attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLateComersByDate(LocalDate date) {
        return attendanceMapper.toResponseList(attendanceRepository.findLateComersByDate(date));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAbsentByDate(LocalDate date) {
        return attendanceMapper.toResponseList(attendanceRepository.findAbsentByDate(date));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getOvertimeByDateRange(LocalDate startDate, LocalDate endDate) {
        return attendanceMapper.toResponseList(attendanceRepository.findOvertimeByDateRange(startDate, endDate));
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

    // =====================================================================
    // Scheduler
    // =====================================================================

    @Override
    @Transactional
    public int autoMarkAbsentForDate(LocalDate date) {
        log.info("Auto-absent sweep for {}", date);
        if (publicHolidayRepository.existsByHolidayDate(date)) {
            log.info("Skipping {} — public holiday", date);
            return 0;
        }

        List<Long> pendingEmployeeIds = attendanceRepository.findActiveEmployeeIdsWithoutAttendance(date);
        if (pendingEmployeeIds.isEmpty()) {
            return 0;
        }

        int marked = 0;
        List<Employee> employees = employeeRepository.findAllById(pendingEmployeeIds);
        for (Employee e : employees) {
            ShiftType shift = resolveShiftForDate(e, date);
            if (isWeeklyOff(shift, date)) {
                // Persist a WEEKLY_OFF row so the calendar UI can show it consistently.
                persistInformationalRow(e, date, shift, AttendanceStatus.WEEKLY_OFF);
                continue;
            }
            // Approved leave on this date → ON_LEAVE, not absent.
            if (hasApprovedLeaveOn(e.getId(), date)) {
                persistInformationalRow(e, date, shift, AttendanceStatus.ON_LEAVE);
                continue;
            }
            // Otherwise: auto-absent. HR can override later.
            Attendance row = Attendance.builder()
                    .employee(e)
                    .attendanceDate(date)
                    .shiftType(shift)
                    .status(AttendanceStatus.AUTO_ABSENT)
                    .approvalStatus(AttendanceApprovalStatus.APPROVED)
                    .isAutoAbsent(true)
                    .build();
            attendanceRepository.save(row);
            marked++;
        }
        log.info("Auto-absent sweep for {}: {} rows marked AUTO_ABSENT", date, marked);
        return marked;
    }

    @Override
    @Transactional
    public int autoCloseMissingPunchesForDate(LocalDate date) {
        List<Attendance> unclosed = attendanceRepository.findUnclosedPunchesForDate(date);
        int flagged = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Attendance a : unclosed) {
            LocalTime end = a.getShiftType() != null && a.getShiftType().getEndTime() != null
                    ? a.getShiftType().getEndTime()
                    : FALLBACK_SHIFT_END;
            LocalDateTime shiftEnd = LocalDateTime.of(date, end);
            // Only flag if we're past shift end for that date.
            if (now.isBefore(shiftEnd)) {
                continue;
            }
            a.setIsMissingPunch(true);
            a.setStatus(AttendanceStatus.MISSING_PUNCH);
            if (a.getApprovalStatus() == AttendanceApprovalStatus.APPROVED) {
                // Send back to approval so HR can correct it.
                a.setApprovalStatus(AttendanceApprovalStatus.PENDING);
                a.setApprovedBy(null);
                a.setApprovedAt(null);
            }
            attendanceRepository.save(a);
            flagged++;
        }
        log.info("Missing-punch sweep for {}: {} rows flagged", date, flagged);
        return flagged;
    }

    @Override
    @Transactional
    public void processDailyAttendance(LocalDate date) {
        // Historical hook kept for the pre-existing scheduler. Run both
        // sweeps so nothing is missed.
        autoMarkAbsentForDate(date);
        autoCloseMissingPunchesForDate(date);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Employee resolveCaller(UserPrincipal principal) {
        if (principal == null || principal.getEmployeeId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return employeeRepository.findByEmployeeId(principal.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", principal.getEmployeeId()));
    }

    private boolean hasAnyAuthority(UserPrincipal principal, String... roles) {
        if (principal == null || principal.getAuthorities() == null) return false;
        for (String role : roles) {
            for (var a : principal.getAuthorities()) {
                if (role.equals(a.getAuthority())) return true;
            }
        }
        return false;
    }

    private ShiftType resolveShiftForDate(Employee employee, LocalDate date) {
        Optional<ShiftAssignment> active = shiftAssignmentRepository
                .findActiveAssignmentForEmployee(employee.getId(), date);
        return active.map(ShiftAssignment::getShiftType).orElse(employee.getShiftType());
    }

    private boolean isWeeklyOff(ShiftType shift, LocalDate date) {
        if (shift == null || shift.getWeeklyOffDays() == null) return false;
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Monday=1..Sunday=0
        return shift.getWeeklyOffDays().contains(dayOfWeek);
    }

    private boolean hasApprovedLeaveOn(Long employeeId, LocalDate date) {
        List<LeaveRequest> overlapping = leaveRequestRepository
                .findApprovedLeavesByEmployeeAndDateRange(employeeId, date, date);
        return !overlapping.isEmpty();
    }

    private void persistInformationalRow(Employee employee, LocalDate date, ShiftType shift, AttendanceStatus status) {
        Attendance row = Attendance.builder()
                .employee(employee)
                .attendanceDate(date)
                .shiftType(shift)
                .status(status)
                .approvalStatus(AttendanceApprovalStatus.APPROVED)
                .build();
        attendanceRepository.save(row);
    }

    private void assertInsideGeofence(Employee employee, PunchRequest request) {
        OfficeLocation office = employee.getOfficeLocation();
        if (office == null || !Boolean.TRUE.equals(office.getEnforceGeofence())) {
            return; // geofence disabled
        }
        if (office.getLatitude() == null || office.getLongitude() == null) {
            // Can't enforce without reference coords — log but allow.
            log.warn("Office {} has geofence enforced but no coordinates set; allowing punch.",
                    office.getName());
            return;
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Location permission is required to punch. Please allow GPS access.");
        }
        double distance = haversineMeters(
                office.getLatitude().doubleValue(),
                office.getLongitude().doubleValue(),
                request.getLatitude().doubleValue(),
                request.getLongitude().doubleValue());
        int radius = office.getGeofenceRadiusMeters() != null ? office.getGeofenceRadiusMeters() : 100;
        if (distance > radius) {
            throw new BadRequestException(String.format(
                    "You are outside the office geofence (%.0f m from %s, allowed %d m). File a regularization request if this is wrong.",
                    distance, office.getName(), radius));
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                  * Math.cos(Math.toRadians(lat2))
                  * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private void checkLateComing(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null || attendance.getPunchIn() == null) return;
        LocalDateTime expectedStart = LocalDateTime.of(attendance.getAttendanceDate(), shift.getStartTime());
        int grace = shift.getGracePeriodMinutes() != null ? shift.getGracePeriodMinutes() : 0;
        LocalDateTime gracePeriodEnd = expectedStart.plusMinutes(grace);
        if (attendance.getPunchIn().isAfter(gracePeriodEnd)) {
            attendance.setIsLate(true);
            attendance.setLateMinutes((int) Duration.between(expectedStart, attendance.getPunchIn()).toMinutes());
        } else {
            attendance.setIsLate(false);
            attendance.setLateMinutes(0);
        }
    }

    private void calculateWorkingHours(Attendance attendance) {
        if (attendance.getPunchIn() == null || attendance.getPunchOut() == null) {
            return;
        }
        Duration duration = Duration.between(attendance.getPunchIn(), attendance.getPunchOut());
        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes() / 60.0)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal breakHours = attendance.getBreakHours() != null ? attendance.getBreakHours() : BigDecimal.ONE;
        BigDecimal working = totalHours.subtract(breakHours).max(BigDecimal.ZERO);
        attendance.setWorkingHours(working);
        attendance.setIsHalfDay(working.compareTo(new BigDecimal("4")) < 0);
    }

    private void checkEarlyLeave(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null || attendance.getPunchOut() == null) return;
        LocalDateTime expectedEnd = LocalDateTime.of(attendance.getAttendanceDate(), shift.getEndTime());
        if (attendance.getPunchOut().isBefore(expectedEnd)) {
            attendance.setIsEarlyLeave(true);
            attendance.setEarlyLeaveMinutes((int) Duration.between(attendance.getPunchOut(), expectedEnd).toMinutes());
        } else {
            attendance.setIsEarlyLeave(false);
            attendance.setEarlyLeaveMinutes(0);
        }
    }

    private void checkOvertime(Attendance attendance) {
        ShiftType shift = attendance.getShiftType();
        if (shift == null || attendance.getWorkingHours() == null) return;
        BigDecimal threshold = shift.getOvertimeThresholdHours() != null
                ? shift.getOvertimeThresholdHours()
                : new BigDecimal("8.00");
        if (attendance.getWorkingHours().compareTo(threshold) > 0) {
            attendance.setIsOvertime(true);
            attendance.setOvertimeHours(attendance.getWorkingHours().subtract(threshold));
        } else {
            attendance.setIsOvertime(false);
            attendance.setOvertimeHours(BigDecimal.ZERO);
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
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
