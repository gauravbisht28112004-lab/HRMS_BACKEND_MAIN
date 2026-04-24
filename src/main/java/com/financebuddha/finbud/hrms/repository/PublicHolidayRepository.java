package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    Optional<PublicHoliday> findByHolidayDate(LocalDate holidayDate);

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<PublicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);

    /** Returns the current calendar year's holidays so the UI has a ready-to-render list. */
    List<PublicHoliday> findAllByOrderByHolidayDateAsc();
}
