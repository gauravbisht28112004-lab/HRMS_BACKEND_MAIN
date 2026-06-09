package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.attendance.RegularizationResponse;
import com.financebuddha.finbud.hrms.entity.RegularizationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegularizationRequestMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeId")
    @Mapping(target = "employeeName",
            expression = "java(request.getEmployee() != null ? request.getEmployee().getFullName() : null)")
    @Mapping(target = "attendanceId", source = "attendance.id")
    @Mapping(target = "reviewedById", source = "reviewedBy.id")
    @Mapping(target = "reviewedByName",
            expression = "java(request.getReviewedBy() != null ? request.getReviewedBy().getFullName() : null)")
    RegularizationResponse toResponse(RegularizationRequest request);

    List<RegularizationResponse> toResponseList(List<RegularizationRequest> requests);
}
