package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayResponse;
import com.financebuddha.finbud.hrms.entity.PublicHoliday;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicHolidayMapper {

    PublicHolidayResponse toResponse(PublicHoliday entity);

    List<PublicHolidayResponse> toResponseList(List<PublicHoliday> entities);
}
