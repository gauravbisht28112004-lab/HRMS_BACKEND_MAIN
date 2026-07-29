package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateResponse;

import java.time.LocalDate;
import java.util.List;

public interface HourlyUpdateService {

    /** Upsert: insert if (employee, date, slot) is new, else update in place. */
    HourlyUpdateResponse upsert(Long employeeId, HourlyUpdateRequest request);

    /** Delete a single hourly update — only the owner can delete. */
    void delete(Long updateId, Long callerEmployeeId);

    /** Caller's own day. */
    List<HourlyUpdateResponse> listMineForDate(Long employeeId, LocalDate workDate);

    /** Caller's range — for weekly reports / personal review. */
    List<HourlyUpdateResponse> listMineForRange(Long employeeId, LocalDate startDate, LocalDate endDate);

    /** TL/HR/Admin: team snapshot for a date. */
    List<HourlyUpdateResponse> listTeamForDate(Long managerId, LocalDate workDate);
}
