package com.financebuddha.finbud.hrms.mapper;

import com.financebuddha.finbud.hrms.config.FinbudStorageProperties;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import com.financebuddha.finbud.hrms.service.ObjectStorageService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

/**
 * MapStruct mapper for Employee &lt;-&gt; DTO.
 * <p>
 * We rely on MapStruct's default same-name matching for the bulk of the
 * ~60 flat fields and only explicitly annotate:
 *   - relationship refs (department / manager / shiftType)
 *   - derived fields (fullName, managerName)
 *   - fields the service layer owns (employeeId, audit timestamps, collections)
 * The {@code unmappedTargetPolicy = IGNORE} keeps MapStruct from failing
 * the build when the DTO omits a given entity field (deliberate for fields
 * like password-hash and statutory numbers on the listing shape).
 * <p>
 * <b>Avatar enrichment.</b> This mapper is an abstract class (not an
 * interface) so it can hold {@link ObjectStorageService} and
 * {@link FinbudStorageProperties} as fields. The {@link #enrichAvatarUrl}
 * {@link AfterMapping} hook runs after every {@code toResponse} call (and
 * therefore also after {@code toDetailResponse}, which delegates via
 * {@code @Mapping(target = "employee", source = ".")}) and replaces
 * {@code profilePictureUrl} with a short-lived presigned GET URL whenever
 * {@link Employee#getAvatarKey()} is set. Behaviour:
 * <pre>
 *   if employee.avatarKey is set → profilePictureUrl = presignedGetUrl(key)
 *   else                          → profilePictureUrl stays as whatever the
 *                                   entity column held (legacy pasted URL)
 * </pre>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EmployeeMapper {

    @Autowired
    protected ObjectStorageService objectStorage;

    @Autowired
    protected FinbudStorageProperties storageProps;

    // ------------------------------------------------------------------
    // Entity -> Response
    // ------------------------------------------------------------------

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(employee.getManager() != null ? employee.getManager().getFullName() : null)")
    @Mapping(target = "shiftTypeId", source = "shiftType.id")
    @Mapping(target = "shiftName", source = "shiftType.name")
    @Mapping(target = "fullName", expression = "java(employee.getFullName())")
    public abstract EmployeeResponse toResponse(Employee employee);

    public abstract List<EmployeeResponse> toResponseList(List<Employee> employees);

    /**
     * Overwrites {@code profilePictureUrl} with a presigned S3 GET URL when
     * the employee has an uploaded avatar. Called by MapStruct after every
     * {@code toResponse}.
     */
    @AfterMapping
    protected void enrichAvatarUrl(Employee employee, @MappingTarget EmployeeResponse response) {
        if (employee == null || response == null) return;
        String key = employee.getAvatarKey();
        if (key == null || key.isBlank()) {
            // Leave whatever the legacy profile_picture_url column had.
            return;
        }
        Duration ttl = Duration.ofSeconds(
                storageProps != null ? storageProps.getPresignTtlSeconds() : 3600L);
        objectStorage.presignedGetUrl(key, ttl)
                .ifPresent(response::setProfilePictureUrl);
    }

    // ------------------------------------------------------------------
    // Request -> new Entity
    // ------------------------------------------------------------------

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
    // avatarKey / avatarContentType are owned by the avatar controller, not
    // the generic create/update API — keep them off the request path.
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "avatarContentType", ignore = true)
    public abstract Employee toEntity(EmployeeRequest request);

    // ------------------------------------------------------------------
    // Request -> existing Entity (partial update)
    // ------------------------------------------------------------------

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
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
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "avatarContentType", ignore = true)
    public abstract void updateEntityFromRequest(EmployeeRequest request, @MappingTarget Employee employee);

    // ------------------------------------------------------------------
    // Entity -> Detail Response (summary + finance sub-sections)
    // ------------------------------------------------------------------

    @Mapping(target = "employee", source = ".")
    @Mapping(target = "salaryInfo", expression = "java(mapSalaryInfo(employee.getSalaryStructure()))")
    @Mapping(target = "bankInfo", expression = "java(mapBankInfo(employee))")
    @Mapping(target = "identityInfo", expression = "java(mapIdentityInfo(employee))")
    public abstract EmployeeDetailResponse toDetailResponse(Employee employee);

    protected EmployeeDetailResponse.SalaryInfo mapSalaryInfo(SalaryStructure salary) {
        if (salary == null) return null;
        return EmployeeDetailResponse.SalaryInfo.builder()
                .salaryStructureId(salary.getId())
                .structureType(salary.getStructureType())
                .monthlyGrossCtc(salary.getMonthlyGrossCtc())
                .nth(salary.getNth())
                .annualCtc(salary.getAnnualCtc())
                .monthlyCtc(salary.getMonthlyCtc())
                .employerPf(salary.getEmployerPf())
                .employeePf(salary.getEmployeePf())
                .employerEsi(salary.getEmployerEsi())
                .employeeEsi(salary.getEmployeeEsi())
                .lwfAmount(salary.getLwfAmount())
                .tdsAmount(salary.getTdsAmount())
                .tdsRatePercent(salary.getTdsRatePercent())
                .incentives(salary.getIncentives())
                .otherDeductions(salary.getOtherDeductions())
                .numOfMonths(salary.getNumOfMonths())
                .effectiveFrom(salary.getEffectiveFrom())
                .effectiveTo(salary.getEffectiveTo())
                .isActive(salary.getIsActive())
                .build();
    }

    protected EmployeeDetailResponse.BankInfo mapBankInfo(Employee employee) {
        if (employee == null) return null;
        return EmployeeDetailResponse.BankInfo.builder()
                .accountNumber(employee.getBankAccountNumber())
                .ifscCode(employee.getBankIfscCode())
                .bankName(employee.getBankName())
                .accountType(employee.getBankAccountType())
                .branch(employee.getBankBranch())
                .salaryPaymentMode(employee.getSalaryPaymentMode())
                .ddPayableAt(employee.getDdPayableAt())
                .nameAsPerBank(employee.getNameAsPerBank())
                .iban(employee.getIban())
                .build();
    }

    protected EmployeeDetailResponse.IdentityInfo mapIdentityInfo(Employee employee) {
        if (employee == null) return null;
        return EmployeeDetailResponse.IdentityInfo.builder()
                .panNumber(employee.getPanNumber())
                .aadhaarNumber(employee.getAadhaarNumber())
                .aadhaarEnrolmentNo(employee.getAadhaarEnrolmentNo())
                .aadhaarName(employee.getAadhaarName())
                .uanNumber(employee.getUanNumber())
                .pfNumber(employee.getPfNumber())
                .pfScheme(employee.getPfScheme())
                .pfJoiningDate(employee.getPfJoiningDate())
                .esiNumber(employee.getEsiNumber())
                .pfEligible(employee.getPfEligible())
                .esiEligible(employee.getEsiEligible())
                .lwfEligible(employee.getLwfEligible())
                .existingPfMember(employee.getExistingPfMember())
                .excessEpfEligible(employee.getExcessEpfEligible())
                .excessEpsEligible(employee.getExcessEpsEligible())
                .build();
    }
}
