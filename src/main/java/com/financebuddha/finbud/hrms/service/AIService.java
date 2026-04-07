package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.ai.AIQueryRequest;
import com.financebuddha.finbud.hrms.dto.ai.AIQueryResponse;

import java.time.LocalDate;

public interface AIService {

    AIQueryResponse processQuery(AIQueryRequest request);

    void indexEmployeeData(Long employeeId);

    void indexAttendanceData(Long attendanceId);

    void indexPayrollData(Long payrollId);

    void indexLeaveData(Long leaveId);

    String generatePayrollSummary(Integer month, Integer year);

    String getLateComersReport(LocalDate date);

    String getAbsentReport(LocalDate date);

    String getOvertimeReport(Integer month, Integer year);
}
