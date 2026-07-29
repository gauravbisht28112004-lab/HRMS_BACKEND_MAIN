package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    Optional<SalaryStructure> findByEmployeeId(Long employeeId);

    Optional<SalaryStructure> findByEmployeeIdAndIsActiveTrue(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);

    // Returns the salary structure valid for a given as-of date.
    // Used by PayrollService when generating payroll for a historical month.
    @Query("SELECT s FROM SalaryStructure s WHERE s.employee.id = :employeeId " +
           "AND s.effectiveFrom <= :asOfDate " +
           "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :asOfDate)")
    Optional<SalaryStructure> findEffectiveForEmployee(@Param("employeeId") Long employeeId,
                                                      @Param("asOfDate") LocalDate asOfDate);

    // All historical structures for an employee, newest first — useful for audit views.
    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
}
