package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.enums.CommitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCommitmentRepository extends JpaRepository<DailyCommitment, Long> {

    Optional<DailyCommitment> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    /** Most recent N rows for an employee (employee history view). */
    List<DailyCommitment> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
            Long employeeId, LocalDate startDate, LocalDate endDate);

    /** All rows for an employee (used by reports). */
    List<DailyCommitment> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    /** Pending-approval queue for a TL — their direct reports' SUBMITTED rows. */
    @Query("""
           SELECT c FROM DailyCommitment c
            WHERE c.employee.manager.id = :managerId
              AND c.status = :status
            ORDER BY c.workDate DESC
           """)
    List<DailyCommitment> findByManagerIdAndStatus(@Param("managerId") Long managerId,
                                                    @Param("status") CommitmentStatus status);

    /** Team-snapshot for a date — used by TL "team progress today" view. */
    @Query("""
           SELECT c FROM DailyCommitment c
            WHERE c.employee.manager.id = :managerId
              AND c.workDate = :workDate
            ORDER BY c.employee.firstName ASC
           """)
    List<DailyCommitment> findByManagerIdAndWorkDate(@Param("managerId") Long managerId,
                                                      @Param("workDate") LocalDate workDate);

    /** Team-history within a date window — used by the team Excel report. */
    @Query("""
           SELECT c FROM DailyCommitment c
            WHERE c.employee.manager.id = :managerId
              AND c.workDate BETWEEN :startDate AND :endDate
            ORDER BY c.workDate DESC, c.employee.firstName ASC
           """)
    List<DailyCommitment> findByManagerIdAndWorkDateBetween(@Param("managerId") Long managerId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    /**
     * Aggregate query for the Q3 leaderboard: total APPROVED disbursal per
     * employee within a date window. Returns rows of (employee_id, total).
     * The service maps to a ranked DTO list.
     */
    @Query("""
           SELECT c.employee.id AS employeeId, COALESCE(SUM(c.actualDisbursalAmount), 0) AS total
             FROM DailyCommitment c
            WHERE c.workDate BETWEEN :startDate AND :endDate
              AND c.status = com.financebuddha.finbud.hrms.enums.CommitmentStatus.APPROVED
            GROUP BY c.employee.id
            ORDER BY total DESC
           """)
    List<LeaderboardRow> aggregateApprovedDisbursalByEmployee(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Projection for the leaderboard aggregate. */
    interface LeaderboardRow {
        Long getEmployeeId();
        BigDecimal getTotal();
    }

    /**
     * Per-employee sum of APPROVED disbursal in a date window. Used by the
     * Monthly Target view to overlay achieved-vs-target. Returns 0 (not
     * null) when the employee has no APPROVED rows in range — keeps the
     * caller code simpler.
     */
    @Query("""
           SELECT COALESCE(SUM(c.actualDisbursalAmount), 0)
             FROM DailyCommitment c
            WHERE c.employee.id = :employeeId
              AND c.workDate BETWEEN :startDate AND :endDate
              AND c.status = com.financebuddha.finbud.hrms.enums.CommitmentStatus.APPROVED
           """)
    BigDecimal sumApprovedDisbursalForEmployee(@Param("employeeId") Long employeeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * ATL rollup: total <em>committed</em> (target, any status) disbursal per
     * manager, grouped over a fixed set of manager ids — used for the
     * HR/Admin "all ATLs" summary. Restricted to an explicit id list (rather
     * than grouping over every manager in the table) so a MANAGER's team
     * doesn't leak into an ATL-only report.
     *
     * <p>Uses {@code targetDisbursalAmount} rather than
     * {@code actualDisbursalAmount} deliberately — this reflects what
     * employees have <em>committed to</em>, independent of TL approval,
     * matching how "commitment" is used elsewhere in this module.
     */
    @Query("""
           SELECT c.employee.manager.id AS managerId, COALESCE(SUM(c.targetDisbursalAmount), 0) AS total
             FROM DailyCommitment c
            WHERE c.employee.manager.id IN :managerIds
              AND c.workDate BETWEEN :startDate AND :endDate
            GROUP BY c.employee.manager.id
           """)
    List<ManagerAggregateRow> aggregateTargetDisbursalByManagerIds(
            @Param("managerIds") List<Long> managerIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * ATL rollup: total APPROVED <em>actual</em> disbursal per manager over a
     * date window, grouped over an explicit manager-id list. Used by the
     * HR/Admin ATL summary's "actual disbursed to date" column so progress
     * against the assigned monthly target is visible at a glance.
     */
    @Query("""
           SELECT c.employee.manager.id AS managerId, COALESCE(SUM(c.actualDisbursalAmount), 0) AS total
             FROM DailyCommitment c
            WHERE c.employee.manager.id IN :managerIds
              AND c.workDate BETWEEN :startDate AND :endDate
              AND c.status = com.financebuddha.finbud.hrms.enums.CommitmentStatus.APPROVED
            GROUP BY c.employee.manager.id
           """)
    List<ManagerAggregateRow> aggregateApprovedActualByManagerIds(
            @Param("managerIds") List<Long> managerIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Projection for the ATL rollup aggregate. */
    interface ManagerAggregateRow {
        Long getManagerId();
        BigDecimal getTotal();
    }
}
