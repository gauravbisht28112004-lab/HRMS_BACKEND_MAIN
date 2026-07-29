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

    /**
     * All assignments for an employee, newest first, for the timeline view.
     */
    List<ShiftAssignment> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    List<ShiftAssignment> findByEmployeeId(Long employeeId);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.employee.id = :employeeId " +
           "AND sa.effectiveFrom <= :date AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date)")
    Optional<ShiftAssignment> findActiveAssignmentForEmployee(@Param("employeeId") Long employeeId,
                                                                  @Param("date") LocalDate date);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.shiftType.id = :shiftTypeId " +
           "AND sa.effectiveFrom <= :date AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date)")
    List<ShiftAssignment> findActiveAssignmentsByShiftType(@Param("shiftTypeId") Long shiftTypeId,
                                                           @Param("date") LocalDate date);

    /**
     * Currently-open assignment for an employee: effectiveTo IS NULL.
     * There should be at most one such row. If a legacy DB happens to have
     * two, we return the most recent by effectiveFrom so the sync stays
     * deterministic.
     */
    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.employee.id = :employeeId " +
           "AND sa.effectiveTo IS NULL " +
           "ORDER BY sa.effectiveFrom DESC")
    List<ShiftAssignment> findOpenAssignmentsForEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find assignments for an employee whose validity window overlaps the
     * supplied [start, end] range. Used for overlap validation when
     * creating or updating an assignment.
     *
     * <p>Two closed intervals overlap iff:
     *     existing.from <= candidate.end AND (existing.to IS NULL OR existing.to >= candidate.start)
     * Caller passes LocalDate.MAX when the candidate's end is open-ended.</p>
     */
    @Query("SELECT sa FROM ShiftAssignment sa " +
           "WHERE sa.employee.id = :employeeId " +
           "  AND sa.effectiveFrom <= :candidateEnd " +
           "  AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :candidateStart)")
    List<ShiftAssignment> findOverlappingByEmployee(@Param("employeeId") Long employeeId,
                                                     @Param("candidateStart") LocalDate candidateStart,
                                                     @Param("candidateEnd") LocalDate candidateEnd);

    /**
     * Flat list by shift type for the Shifts admin page filter.
     */
    List<ShiftAssignment> findByShiftTypeIdOrderByEffectiveFromDesc(Long shiftTypeId);

    /**
     * Existence check used before deleting a shift type.
     */
    boolean existsByShiftTypeId(Long shiftTypeId);
}
