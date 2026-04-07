package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureRequest;
import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureResponse;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalaryStructureMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", expression = "java(salaryStructure.getEmployee() != null ? salaryStructure.getEmployee().getFullName() : null)")
    @Mapping(target = "totalAllowances", expression = "java(calculateTotalAllowances(salaryStructure))")
    @Mapping(target = "monthlyCtc", expression = "java(salaryStructure.getMonthlyCtc())")
    SalaryStructureResponse toResponse(SalaryStructure salaryStructure);

    List<SalaryStructureResponse> toResponseList(List<SalaryStructure> salaryStructures);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    SalaryStructure toEntity(SalaryStructureRequest request);

    default BigDecimal calculateTotalAllowances(SalaryStructure salary) {
        if (salary == null) return BigDecimal.ZERO;
        return salary.getDa().add(salary.getConveyanceAllowance())
                .add(salary.getMedicalAllowance())
                .add(salary.getSpecialAllowance());
    }
}
