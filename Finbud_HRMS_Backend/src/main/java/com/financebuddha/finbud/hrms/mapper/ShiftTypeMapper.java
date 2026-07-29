package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeRequest;
import com.financebuddha.finbud.hrms.dto.shift.ShiftTypeResponse;
import com.financebuddha.finbud.hrms.entity.ShiftType;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShiftTypeMapper {

    ShiftTypeResponse toResponse(ShiftType shiftType);

    List<ShiftTypeResponse> toResponseList(List<ShiftType> shiftTypes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ShiftType toEntity(ShiftTypeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(ShiftTypeRequest request, @MappingTarget ShiftType shiftType);
}
