package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.HourlyUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HourlyUpdateRepository extends JpaRepository<HourlyUpdate, Long> {

    /** Existing row for upsert behaviour — same slot → update, not duplicate. */
    Optional<HourlyUpdate> findByEmployeeIdAndWorkDateAndHourSlot(
            Long employeeId, LocalDate workDate, String hourSlot);

    /** All rows for an employee on a given date, ordered by hour slot. */
    List<HourlyUpdate> findByEmployeeIdAndWorkDateOrderByHourSlotAsc(Long employeeId, LocalDate workDate);

    /** Rows in a date window — used by reports. */
    List<HourlyUpdate> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDescHourSlotAsc(
            Long employeeId, LocalDate startDate, LocalDate endDate);

    /** Team snapshot for a date — TL view. */
    @Query("""
           SELECT h FROM HourlyUpdate h
            WHERE h.employee.manager.id = :managerId
              AND h.workDate = :workDate
            ORDER BY h.employee.firstName ASC, h.hourSlot ASC
           """)
    List<HourlyUpdate> findByManagerIdAndWorkDate(@Param("managerId") Long managerId,
                                                    @Param("workDate") LocalDate workDate);
}
