package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationResponse;
import com.financebuddha.finbud.hrms.entity.OfficeLocation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfficeLocationMapper {

    OfficeLocationResponse toResponse(OfficeLocation entity);

    List<OfficeLocationResponse> toResponseList(List<OfficeLocation> entities);
}
