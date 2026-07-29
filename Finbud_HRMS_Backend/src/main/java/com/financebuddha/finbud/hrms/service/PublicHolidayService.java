package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayRequest;
import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayResponse;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayService {

    PublicHolidayResponse create(PublicHolidayRequest request);

    PublicHolidayResponse update(Long id, PublicHolidayRequest request);

    void delete(Long id);

    PublicHolidayResponse getById(Long id);

    List<PublicHolidayResponse> listAll();

    List<PublicHolidayResponse> listByDateRange(LocalDate startDate, LocalDate endDate);

    List<PublicHolidayResponse> listByYear(int year);
}
