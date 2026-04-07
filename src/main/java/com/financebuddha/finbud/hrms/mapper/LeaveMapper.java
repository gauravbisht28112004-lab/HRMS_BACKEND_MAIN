package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
import com.financebuddha.finbud.hrms.dto.leave.LeaveRequestDTO;
import com.financebuddha.finbud.hrms.dto.leave.LeaveResponse;
import com.financebuddha.finbud.hrms.entity.LeaveBalance;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeaveMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", expression = "java(leaveRequest.getEmployee() != null ? leaveRequest.getEmployee().getFullName() : null)")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(leaveRequest.getManager() != null ? leaveRequest.getManager().getFullName() : null)")
    @Mapping(target = "approvedBy", expression = "java(leaveRequest.getApprovedBy() != null ? leaveRequest.getApprovedBy().getFullName() : null)")
    LeaveResponse toResponse(LeaveRequest leaveRequest);

    List<LeaveResponse> toResponseList(List<LeaveRequest> leaveRequests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    LeaveRequest toEntity(LeaveRequestDTO leaveRequestDTO);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", expression = "java(leaveBalance.getEmployee() != null ? leaveBalance.getEmployee().getFullName() : null)")
    @Mapping(target = "casualLeaveBalance", expression = "java(leaveBalance.getCasualLeaveBalance())")
    @Mapping(target = "sickLeaveBalance", expression = "java(leaveBalance.getSickLeaveBalance())")
    @Mapping(target = "paidLeaveBalance", expression = "java(leaveBalance.getPaidLeaveBalance())")
    @Mapping(target = "wfhDaysBalance", expression = "java(leaveBalance.getWfhBalance())")
    LeaveBalanceResponse toBalanceResponse(LeaveBalance leaveBalance);
}
