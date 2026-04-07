package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Payroll;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<Payroll> findByEmployeeIdAndYear(Long employeeId, Integer year);

    Page<Payroll> findByMonthAndYear(Integer month, Integer year, Pageable pageable);

    List<Payroll> findByMonthAndYearAndStatus(Integer month, Integer year, PayrollStatus status);

    Page<Payroll> findByStatus(PayrollStatus status, Pageable pageable);

    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    @Query("SELECT SUM(p.netPay) FROM Payroll p WHERE p.month = :month AND p.year = :year AND p.status = 'PAID'")
    Double sumNetPayByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT SUM(p.grossEarnings) FROM Payroll p WHERE p.month = :month AND p.year = :year")
    Double sumGrossEarningsByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT SUM(p.totalDeductions) FROM Payroll p WHERE p.month = :month AND p.year = :year")
    Double sumTotalDeductionsByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);
}
