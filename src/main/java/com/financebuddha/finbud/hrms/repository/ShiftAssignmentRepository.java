package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByEmployeeId(Long employeeId);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.employee.id = :employeeId " +
           "AND sa.effectiveFrom <= :date AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date)")
    Optional<ShiftAssignment> findActiveAssignmentForEmployee(@Param("employeeId") Long employeeId,
                                                                  @Param("date") LocalDate date);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.shiftType.id = :shiftTypeId " +
           "AND sa.effectiveFrom <= :date AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date)")
    List<ShiftAssignment> findActiveAssignmentsByShiftType(@Param("shiftTypeId") Long shiftTypeId,
                                                           @Param("date") LocalDate date);
}
