package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.department.DepartmentRequest;
import com.financebuddha.finbud.hrms.dto.department.DepartmentResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper {

    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(department.getManager() != null ? department.getManager().getFullName() : null)")
    DepartmentResponse toResponse(Department department);

    List<DepartmentResponse> toResponseList(List<Department> departments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Department toEntity(DepartmentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Department department);
}
