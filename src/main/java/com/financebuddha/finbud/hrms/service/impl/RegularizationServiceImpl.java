package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.attendance.RegularizationRequestDto;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationResponse;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationReviewRequest;
import com.financebuddha.finbud.hrms.entity.Attendance;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.RegularizationRequest;
import com.financebuddha.finbud.hrms.enums.AttendanceApprovalStatus;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import com.financebuddha.finbud.hrms.enums.RegularizationStatus;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.RegularizationRequestMapper;
import com.financebuddha.finbud.hrms.repository.AttendanceRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RegularizationRequestRepository;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.RegularizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegularizationServiceImpl implements RegularizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HR = "ROLE_HR";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    private final RegularizationRequestRepository regularizationRepository;
    private final RegularizationRequestMapper regularizationMapper;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    @Transactional
    public RegularizationResponse submit(RegularizationRequestDto request, UserPrincipal principal) {
        Employee employee = resolveCaller(principal);

        if (request.getRequestedPunchIn() == null && request.getRequestedPunchOut() == null) {
            throw new BadRequestException(
                    "At least one of requestedPunchIn / requestedPunchOut must be provided");
        }

        if (request.getRequestedPunchIn() != null
                && request.getRequestedPunchOut() != null
                && !request.getRequestedPunchOut().isAfter(request.getRequestedPunchIn())) {
            throw new BadRequestException("requestedPunchOut must be after requestedPunchIn");
        }

        // Guard against duplicate open requests for the same day.
        regularizationRepository.findFirstByEmployeeIdAndAttendanceDateAndStatus(
                        employee.getId(), request.getAttendanceDate(), RegularizationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "You already have a pending regularization for " + request.getAttendanceDate());
                });

        // Link to the existing attendance row for that date, if present.
        Attendance linked = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), request.getAttendanceDate())
                .orElse(null);

        RegularizationRequest entity = RegularizationRequest.builder()
                .employee(employee)
                .attendance(linked)
                .attendanceDate(request.getAttendanceDate())
                .requestedPunchIn(request.getRequestedPunchIn())
                .requestedPunchOut(request.getRequestedPunchOut())
                .reason(request.getReason())
                .status(RegularizationStatus.PENDING)
                .build();

        RegularizationRequest saved = regularizationRepository.save(entity);
        log.info("Employee {} submitted regularization id={} for date={}",
                employee.getEmployeeId(), saved.getId(), saved.getAttendanceDate());
        return regularizationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOwnRequest(Long id, UserPrincipal principal) {
        Employee employee = resolveCaller(principal);
        RegularizationRequest entity = regularizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization", "id", id));

        if (!entity.getEmployee().getId().equals(employee.getId())) {
            throw new ForbiddenException("You can only cancel your own regularization requests");
        }
        if (entity.getStatus() != RegularizationStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be cancelled");
        }

        regularizationRepository.delete(entity);
        log.info("Employee {} cancelled regularization id={}", employee.getEmployeeId(), id);
    }

    @Override
    @Transactional
    public RegularizationResponse review(Long id, RegularizationReviewRequest request, UserPrincipal principal) {
        Employee reviewer = resolveCaller(principal);
        RegularizationRequest entity = regularizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization", "id", id));

        if (entity.getStatus() != RegularizationStatus.PENDING) {
            throw new BadRequestException("Regularization is already " + entity.getStatus());
        }

        // Scope check: TL can only review direct reports. HR/Admin can review anyone.
        boolean isHrOrAdmin = hasAnyAuthority(principal, ROLE_HR, ROLE_ADMIN);
        if (!isHrOrAdmin) {
            boolean isManager = hasAnyAuthority(principal, ROLE_MANAGER);
            Employee subject = entity.getEmployee();
            Long subjectManagerId = subject.getManager() != null ? subject.getManager().getId() : null;
            if (!isManager || subjectManagerId == null || !subjectManagerId.equals(reviewer.getId())) {
                throw new ForbiddenException("You are not authorised to review this regularization request");
            }
        }

        boolean approve = Boolean.TRUE.equals(request.getApprove());
        if (!approve) {
            if (request.getReviewNotes() == null || request.getReviewNotes().isBlank()) {
                throw new BadRequestException("Review notes are required when rejecting a request");
            }
        }

        entity.setReviewedBy(reviewer);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(request.getReviewNotes());

        if (approve) {
            entity.setStatus(RegularizationStatus.APPROVED);
            applyApprovedRegularizationToAttendance(entity, reviewer);
        } else {
            entity.setStatus(RegularizationStatus.REJECTED);
        }

        RegularizationRequest saved = regularizationRepository.save(entity);
        log.info("Reviewer {} {} regularization id={}",
                reviewer.getEmployeeId(), approve ? "APPROVED" : "REJECTED", saved.getId());
        return regularizationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegularizationResponse> listMyRequests(UserPrincipal principal) {
        Employee employee = resolveCaller(principal);
        return regularizationMapper.toResponseList(
                regularizationRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegularizationResponse> listPendingForApprover(UserPrincipal principal) {
        Employee reviewer = resolveCaller(principal);
        if (hasAnyAuthority(principal, ROLE_HR, ROLE_ADMIN)) {
            return regularizationMapper.toResponseList(
                    regularizationRepository.findByStatusOrderByCreatedAtDesc(RegularizationStatus.PENDING));
        }
        if (hasAnyAuthority(principal, ROLE_MANAGER)) {
            return regularizationMapper.toResponseList(
                    regularizationRepository.findByStatusAndManager(
                            RegularizationStatus.PENDING, reviewer.getId()));
        }
        throw new ForbiddenException("You are not authorised to view the approval queue");
    }

    @Override
    @Transactional(readOnly = true)
    public RegularizationResponse getById(Long id, UserPrincipal principal) {
        Employee caller = resolveCaller(principal);
        RegularizationRequest entity = regularizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization", "id", id));

        boolean isHrOrAdmin = hasAnyAuthority(principal, ROLE_HR, ROLE_ADMIN);
        boolean isOwner = entity.getEmployee().getId().equals(caller.getId());
        boolean isManagerOfSubject = hasAnyAuthority(principal, ROLE_MANAGER)
                && entity.getEmployee().getManager() != null
                && caller.getId().equals(entity.getEmployee().getManager().getId());

        if (!(isHrOrAdmin || isOwner || isManagerOfSubject)) {
            throw new ForbiddenException("You are not authorised to view this regularization request");
        }
        return regularizationMapper.toResponse(entity);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * On approval, reflect the requested punches into the Attendance table so
     * downstream systems (payroll, reports) see the corrected day. Upserts
     * the row and marks it APPROVED with the reviewer as manuallyEditedBy.
     */
    private void applyApprovedRegularizationToAttendance(RegularizationRequest entity, Employee reviewer) {
        Attendance attendance = entity.getAttendance();
        if (attendance == null) {
            attendance = attendanceRepository
                    .findByEmployeeIdAndAttendanceDate(entity.getEmployee().getId(), entity.getAttendanceDate())
                    .orElse(null);
        }
        if (attendance == null) {
            attendance = Attendance.builder()
                    .employee(entity.getEmployee())
                    .attendanceDate(entity.getAttendanceDate())
                    .shiftType(entity.getEmployee().getShiftType())
                    .status(AttendanceStatus.PRESENT)
                    .build();
        }

        if (entity.getRequestedPunchIn() != null) {
            attendance.setPunchIn(entity.getRequestedPunchIn());
        }
        if (entity.getRequestedPunchOut() != null) {
            attendance.setPunchOut(entity.getRequestedPunchOut());
        }

        if (attendance.getPunchIn() != null && attendance.getPunchOut() != null) {
            Duration duration = Duration.between(attendance.getPunchIn(), attendance.getPunchOut());
            BigDecimal breakHours = attendance.getBreakHours() != null ? attendance.getBreakHours() : BigDecimal.ONE;
            BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal working = totalHours.subtract(breakHours);
            if (working.compareTo(BigDecimal.ZERO) < 0) {
                working = BigDecimal.ZERO;
            }
            attendance.setWorkingHours(working);
            attendance.setIsHalfDay(working.compareTo(BigDecimal.valueOf(4)) < 0);
            attendance.setStatus(AttendanceStatus.PRESENT);
        }

        attendance.setApprovalStatus(AttendanceApprovalStatus.APPROVED);
        attendance.setApprovedBy(reviewer);
        attendance.setApprovedAt(LocalDateTime.now());
        attendance.setRejectionReason(null);
        attendance.setIsAutoAbsent(false);
        attendance.setIsMissingPunch(false);
        attendance.setManuallyEditedBy(reviewer);
        attendance.setManuallyEditedAt(LocalDateTime.now());
        if (attendance.getNotes() == null || attendance.getNotes().isBlank()) {
            attendance.setNotes("Regularization approved: " + entity.getReason());
        }

        Attendance saved = attendanceRepository.save(attendance);
        entity.setAttendance(saved);
    }

    private Employee resolveCaller(UserPrincipal principal) {
        if (principal == null || principal.getEmployeeId() == null) {
            throw new ForbiddenException("Authenticated principal is not linked to an employee");
        }
        return employeeRepository.findByEmployeeId(principal.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee", "employeeId", principal.getEmployeeId()));
    }

    private boolean hasAnyAuthority(UserPrincipal principal, String... roles) {
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : principal.getAuthorities()) {
            for (String role : roles) {
                if (role.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
