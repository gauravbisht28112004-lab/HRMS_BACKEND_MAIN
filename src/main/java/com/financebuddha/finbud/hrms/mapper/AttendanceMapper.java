package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.entity.Attendance;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeId")
    @Mapping(target = "employeeName",
            expression = "java(attendance.getEmployee() != null ? attendance.getEmployee().getFullName() : null)")
    @Mapping(target = "department",
            expression = "java(attendance.getEmployee() != null && attendance.getEmployee().getDepartment() != null ? attendance.getEmployee().getDepartment().getName() : null)")
    @Mapping(target = "designation",
            expression = "java(attendance.getEmployee() != null ? attendance.getEmployee().getDesignation() : null)")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByName",
            expression = "java(attendance.getApprovedBy() != null ? attendance.getApprovedBy().getFullName() : null)")
    @Mapping(target = "manuallyEditedById", source = "manuallyEditedBy.id")
    @Mapping(target = "manuallyEditedByName",
            expression = "java(attendance.getManuallyEditedBy() != null ? attendance.getManuallyEditedBy().getFullName() : null)")
    AttendanceResponse toResponse(Attendance attendance);

    List<AttendanceResponse> toResponseList(List<Attendance> attendances);
}
