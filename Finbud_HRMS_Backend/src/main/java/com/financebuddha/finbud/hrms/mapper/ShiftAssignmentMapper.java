package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.shift.ShiftAssignmentResponse;
import com.financebuddha.finbud.hrms.entity.ShiftAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.List;

/**
 * MapStruct mapper for ShiftAssignment → ShiftAssignmentResponse.
 *
 * <p>We do not expose a `toEntity` here because the service layer owns
 * the resolution of Employee and ShiftType by id (fetched from their
 * respective repositories), and the `current` flag is a derived, not
 * stored, value.</p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShiftAssignmentMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeId")
    @Mapping(target = "employeeName",
            expression = "java(assignment.getEmployee() != null ? assignment.getEmployee().getFullName() : null)")
    @Mapping(target = "shiftTypeId", source = "shiftType.id")
    @Mapping(target = "shiftTypeCode", source = "shiftType.code")
    @Mapping(target = "shiftTypeName", source = "shiftType.name")
    @Mapping(target = "current", expression = "java(isCurrent(assignment))")
    ShiftAssignmentResponse toResponse(ShiftAssignment assignment);

    List<ShiftAssignmentResponse> toResponseList(List<ShiftAssignment> assignments);

    /**
     * A row is "current" if its validity window contains today. An absent
     * effectiveTo means the window is open-ended.
     */
    default boolean isCurrent(ShiftAssignment assignment) {
        if (assignment == null || assignment.getEffectiveFrom() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (assignment.getEffectiveFrom().isAfter(today)) {
            return false;
        }
        return assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today);
    }
}
