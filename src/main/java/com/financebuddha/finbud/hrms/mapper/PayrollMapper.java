package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.payroll.PayrollResponse;
import com.financebuddha.finbud.hrms.entity.Payroll;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PayrollMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", expression = "java(payroll.getEmployee() != null ? payroll.getEmployee().getFullName() : null)")
    @Mapping(target = "employeeIdCode", expression = "java(payroll.getEmployee() != null ? payroll.getEmployee().getEmployeeId() : null)")
    @Mapping(target = "departmentName", expression = "java(payroll.getEmployee() != null && payroll.getEmployee().getDepartment() != null ? payroll.getEmployee().getDepartment().getName() : null)")
    @Mapping(target = "monthYear", expression = "java(String.valueOf(payroll.getMonth()) + \"/\" + String.valueOf(payroll.getYear()))")
    @Mapping(target = "approvedBy", expression = "java(payroll.getApprovedBy() != null ? payroll.getApprovedBy().getFullName() : null)")
    PayrollResponse toResponse(Payroll payroll);

    List<PayrollResponse> toResponseList(List<Payroll> payrolls);
}
