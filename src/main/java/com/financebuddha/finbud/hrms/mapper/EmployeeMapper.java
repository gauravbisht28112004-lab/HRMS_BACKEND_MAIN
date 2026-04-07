package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(employee.getManager() != null ? employee.getManager().getFullName() : null)")
    @Mapping(target = "shiftTypeId", source = "shiftType.id")
    @Mapping(target = "shiftName", source = "shiftType.name")
    @Mapping(target = "fullName", expression = "java(employee.getFullName())")
    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponseList(List<Employee> employees);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "shiftType", ignore = true)
    @Mapping(target = "salaryStructure", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "attendances", ignore = true)
    @Mapping(target = "leaveRequests", ignore = true)
    @Mapping(target = "payrolls", ignore = true)
    @Mapping(target = "leaveBalances", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    @Mapping(target = "subordinates", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Employee toEntity(EmployeeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "shiftType", ignore = true)
    @Mapping(target = "salaryStructure", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "attendances", ignore = true)
    @Mapping(target = "leaveRequests", ignore = true)
    @Mapping(target = "payrolls", ignore = true)
    @Mapping(target = "leaveBalances", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    @Mapping(target = "subordinates", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(EmployeeRequest request, @MappingTarget Employee employee);

    @Mapping(target = "employee", source = ".")
    @Mapping(target = "salaryInfo", expression = "java(mapSalaryInfo(employee.getSalaryStructure()))")
    @Mapping(target = "bankInfo.accountNumber", source = "bankAccountNumber")
    @Mapping(target = "bankInfo.ifscCode", source = "bankIfscCode")
    @Mapping(target = "bankInfo.bankName", source = "bankName")
    @Mapping(target = "identityInfo.panNumber", source = "panNumber")
    @Mapping(target = "identityInfo.aadhaarNumber", source = "aadhaarNumber")
    EmployeeDetailResponse toDetailResponse(Employee employee);

    default EmployeeDetailResponse.SalaryInfo mapSalaryInfo(SalaryStructure salary) {
        if (salary == null) return null;
        return EmployeeDetailResponse.SalaryInfo.builder()
                .salaryStructureId(salary.getId().toString())
                .annualCtc(salary.getAnnualCtc() != null ? salary.getAnnualCtc().toString() : null)
                .monthlyCtc(salary.getMonthlyCtc() != null ? salary.getMonthlyCtc().toString() : null)
                .build();
    }
}
