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

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(SalaryStructureRequest request, @MappingTarget SalaryStructure salaryStructure);

    /**
     * Legacy allowance calculation — safe to call on CTC-model rows
     * because every component defaults to ZERO in the entity. Components
     * may still be null if the DB row pre-dates V2 migration, so we
     * coalesce defensively.
     */
    default BigDecimal calculateTotalAllowances(SalaryStructure salary) {
        if (salary == null) return BigDecimal.ZERO;
        return nz(salary.getDa())
                .add(nz(salary.getConveyanceAllowance()))
                .add(nz(salary.getMedicalAllowance()))
                .add(nz(salary.getSpecialAllowance()));
    }

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
