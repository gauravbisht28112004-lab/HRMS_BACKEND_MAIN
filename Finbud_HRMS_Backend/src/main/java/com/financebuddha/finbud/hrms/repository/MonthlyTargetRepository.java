package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.MonthlyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyTargetRepository extends JpaRepository<MonthlyTarget, Long> {

    Optional<MonthlyTarget> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    /** Batch target lookup for a set of employees in one period — used by the
     *  hierarchy dashboard to fetch every direct report's target in one query. */
    List<MonthlyTarget> findByEmployeeIdInAndYearAndMonth(List<Long> employeeIds, Integer year, Integer month);

    /** All targets a TL has set / inherited for their direct reports for a period. */
    @Query("""
           SELECT t FROM MonthlyTarget t
            WHERE t.employee.manager.id = :managerId
              AND t.year = :year
              AND t.month = :month
            ORDER BY t.employee.firstName ASC
           """)
    List<MonthlyTarget> findByManagerAndPeriod(@Param("managerId") Long managerId,
                                                 @Param("year") Integer year,
                                                 @Param("month") Integer month);

    // -----------------------------------------------------------------------
    // ATL dashboard rollups — sum of the ASSIGNED monthly target disbursal.
    //
    // A "period" is encoded as (year * 12 + month) so an inclusive month span
    // is a simple BETWEEN. Callers derive the span from a date range:
    //   startPeriod = startDate.year * 12 + startDate.month
    //   endPeriod   = endDate.year   * 12 + endDate.month
    // -----------------------------------------------------------------------

    /**
     * Per-manager sum of assigned monthly target disbursal over an inclusive
     * period span, grouped over an explicit manager-id list. Used by the
     * HR/Admin "ATL summary" rollup. One row per manager that has any target
     * in range.
     */
    @Query("""
           SELECT t.employee.manager.id AS managerId, COALESCE(SUM(t.targetDisbursalAmount), 0) AS total
             FROM MonthlyTarget t
            WHERE t.employee.manager.id IN :managerIds
              AND (t.year > :startYear OR (t.year = :startYear AND t.month >= :startMonth))
              AND (t.year < :endYear OR (t.year = :endYear AND t.month <= :endMonth))
            GROUP BY t.employee.manager.id
           """)
    List<ManagerTargetAggregateRow> aggregateTargetByManagerIds(@Param("managerIds") List<Long> managerIds,
                                                                 @Param("startYear") int startYear,
                                                                 @Param("startMonth") int startMonth,
                                                                 @Param("endYear") int endYear,
                                                                 @Param("endMonth") int endMonth);

    /** Projection for the per-manager monthly-target rollup. */
    interface ManagerTargetAggregateRow {
        Long getManagerId();
        BigDecimal getTotal();
    }

    /**
     * Per-employee sum of assigned monthly target disbursal for one manager's
     * direct reports over an inclusive period span. Used by the per-ATL team
     * dashboard breakdown.
     */
    @Query("""
           SELECT t.employee.id AS employeeId, COALESCE(SUM(t.targetDisbursalAmount), 0) AS total
             FROM MonthlyTarget t
            WHERE t.employee.manager.id = :managerId
              AND (t.year > :startYear OR (t.year = :startYear AND t.month >= :startMonth))
              AND (t.year < :endYear OR (t.year = :endYear AND t.month <= :endMonth))
            GROUP BY t.employee.id
           """)
    List<EmployeeTargetAggregateRow> aggregateTargetByEmployeeForManager(@Param("managerId") Long managerId,
                                                                          @Param("startYear") int startYear,
                                                                          @Param("startMonth") int startMonth,
                                                                          @Param("endYear") int endYear,
                                                                          @Param("endMonth") int endMonth);

    /** Projection for the per-employee monthly-target rollup. */
    interface EmployeeTargetAggregateRow {
        Long getEmployeeId();
        BigDecimal getTotal();
    }
}
