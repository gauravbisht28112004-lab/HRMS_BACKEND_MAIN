package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.MonthlyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyTargetRepository extends JpaRepository<MonthlyTarget, Long> {

    Optional<MonthlyTarget> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

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
}
