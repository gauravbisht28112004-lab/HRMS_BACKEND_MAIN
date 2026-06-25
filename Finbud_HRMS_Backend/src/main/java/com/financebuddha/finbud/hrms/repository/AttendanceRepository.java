package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Attendance;
import com.financebuddha.finbud.hrms.enums.AttendanceApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    Page<Attendance> findByEmployeeId(Long employeeId, Pageable pageable);

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.isLate = true")
    List<Attendance> findLateComersByDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.status = 'ABSENT'")
    List<Attendance> findAbsentByDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate BETWEEN :startDate AND :endDate AND a.isOvertime = true")
    List<Attendance> findOvertimeByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ---------------------------------------------------------------------
    // Reports dashboard exports — org-wide, date-range, optional department.
    // ---------------------------------------------------------------------

    /** All attendance rows in the window, optionally scoped to a department. */
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.attendanceDate BETWEEN :startDate AND :endDate
              AND (:departmentId IS NULL OR a.employee.department.id = :departmentId)
            ORDER BY a.attendanceDate DESC, a.employee.id ASC
            """)
    List<Attendance> findForReport(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("departmentId") Long departmentId);

    /** Overtime-only rows in the window, optionally scoped to a department. */
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.attendanceDate BETWEEN :startDate AND :endDate
              AND a.isOvertime = true
              AND (:departmentId IS NULL OR a.employee.department.id = :departmentId)
            ORDER BY a.attendanceDate DESC, a.employee.id ASC
            """)
    List<Attendance> findOvertimeForReport(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.status = 'PRESENT'")
    Long countPresentDaysByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.isLate = true")
    Long countLateDaysByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.isHalfDay = true")
    Long countHalfDaysByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Query("SELECT SUM(a.overtimeHours) FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Double sumOvertimeHoursByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    // ---------------------------------------------------------------------
    // Approval workflow helpers
    // ---------------------------------------------------------------------

    /** Approval inbox — all pending rows across the org, for HR/Admin. */
    List<Attendance> findByApprovalStatusOrderByAttendanceDateDescIdDesc(AttendanceApprovalStatus status);

    /** Approval inbox scoped to one manager's direct reports, for TL. */
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.approvalStatus = :status
              AND a.employee.manager.id = :managerId
            ORDER BY a.attendanceDate DESC, a.id DESC
            """)
    List<Attendance> findByStatusAndManager(@Param("status") AttendanceApprovalStatus status,
                                            @Param("managerId") Long managerId);

    /** Daily driver for the nightly auto-absent scheduler. */
    @Query("""
            SELECT e.id FROM Employee e
            WHERE e.status = com.financebuddha.finbud.hrms.enums.EmployeeStatus.ACTIVE
              AND NOT EXISTS (
                SELECT 1 FROM Attendance a
                WHERE a.employee.id = e.id AND a.attendanceDate = :date
              )
            """)
    List<Long> findActiveEmployeeIdsWithoutAttendance(@Param("date") LocalDate date);

    /** Rows where employee punched in but never punched out for a given date. */
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.attendanceDate = :date
              AND a.punchIn IS NOT NULL
              AND a.punchOut IS NULL
              AND a.isMissingPunch = false
            """)
    List<Attendance> findUnclosedPunchesForDate(@Param("date") LocalDate date);
}
