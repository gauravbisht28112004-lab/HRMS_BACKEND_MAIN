package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetResponse;
import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetUpsertRequest;

import java.util.List;

public interface MonthlyTargetService {

    /**
     * Set / update an employee's monthly target. {@code setterEmployeeId}
     * is recorded so the UI can show "set by Anjali Bisht (HR) 3 days ago".
     */
    MonthlyTargetResponse upsert(Long employeeId, Long setterEmployeeId, MonthlyTargetUpsertRequest request);

    /** Read for one employee + period. Returns a zero-target placeholder if none exists yet. */
    MonthlyTargetResponse get(Long employeeId, Integer year, Integer month);

    /** Team list for a TL — direct reports' targets for a period, with achieved overlay. */
    List<MonthlyTargetResponse> listForManager(Long managerId, Integer year, Integer month);
}
