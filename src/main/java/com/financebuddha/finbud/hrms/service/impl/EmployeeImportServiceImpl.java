package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.imports.EmployeeImportDTO;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.BackgroundCheckStatus;
import com.financebuddha.finbud.hrms.enums.BloodGroup;
import com.financebuddha.finbud.hrms.enums.EmployeeCategory;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import com.financebuddha.finbud.hrms.enums.Gender;
import com.financebuddha.finbud.hrms.enums.MaritalStatus;
import com.financebuddha.finbud.hrms.enums.ProducerType;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import com.financebuddha.finbud.hrms.repository.DepartmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.SalaryStructureRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.EmployeeImportService;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical two-pass employee importer. See
 * {@link EmployeeImportService} for the contract and responsibilities.
 * <p>
 * Replaces the earlier inline role-assignment logic in
 * {@link ExcelImportServiceImpl} (ADMIN_CODE / HR_CODE / hard-coded
 * password). Those hard-codes were a security smell — every import run
 * would silently re-grant admin rights to a specific employee ID.
 * The new contract is: always provision ROLE_EMPLOYEE, never auto-grant
 * ADMIN/HR, always read the default password from
 * {@link SystemConfigService.Keys#AUTH_DEFAULT_PASSWORD}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeImportServiceImpl implements EmployeeImportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfig;
    /**
     * Needed so each row can be persisted inside its own REQUIRES_NEW
     * transaction. If one row blows up (e.g. a unique-constraint collision
     * on a user account), we roll back only that row's mini-transaction —
     * the Hibernate session the next row uses is fresh, so Hibernate
     * doesn't panic with {@code AssertionFailure: null id ... (don't flush
     * the Session after an exception occurs)} when the outer session was
     * already poisoned by the earlier failure.
     */
    private final PlatformTransactionManager transactionManager;

    /**
     * Needed for dry-run updates: when we load an existing Employee to apply
     * changes for validation only, we must {@code detach()} it so our
     * in-memory mutations never reach the database on autoflush/commit.
     */
    @PersistenceContext
    private EntityManager entityManager;

    // Default fallbacks — used only when the matching system_config key is
    // missing or blank. The real values live in the DB (seeded by Flyway V5).
    private static final String FALLBACK_DEFAULT_PASSWORD = "Welcome@123";
    private static final String FALLBACK_DEFAULT_ROLE     = "ROLE_EMPLOYEE";
    private static final String FALLBACK_DEFAULT_COUNTRY  = "India";
    private static final String FALLBACK_DEFAULT_LOCATION = "Noida";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy")
    );

    /**
     * Previously the whole import ran inside a single {@code @Transactional}
     * boundary. That turned out to be fragile: if any single row threw a
     * persistence exception mid-loop, the Hibernate Session became poisoned
     * (a half-created Employee with {@code id=null} left behind) and every
     * subsequent row's autoflush crashed with
     * {@code AssertionFailure: null id ... (don't flush the Session after
     * an exception occurs)} → the whole 429-row batch rolled back and the
     * controller returned HTTP 500.
     * <p>
     * New contract: each row gets its own REQUIRES_NEW nested transaction.
     * A failed row rolls back only its own mini-transaction, the next row
     * opens a fresh Session, and previously-committed rows stay committed.
     * Pass 2 (manager resolution) runs in its own nested transaction after
     * pass 1 settles so the directory lookups see the just-imported rows.
     * <p>
     * The top-level method is deliberately NOT annotated with
     * {@code @Transactional} — we want to manage transactions manually
     * through {@link TransactionTemplate} so every row is isolated.
     */
    @Override
    public ImportResponse importEmployees(List<EmployeeImportDTO> rows, Options options) {
        if (options == null) {
            options = Options.builder().build();
        }
        if (rows == null) {
            rows = new ArrayList<>();
        }

        final Options opts = options;
        final ImportResponse response = ImportResponse.builder()
                .dryRun(opts.isDryRun())
                .includeResigned(opts.isIncludeResigned())
                .totalRecords(rows.size())
                .build();

        log.info("EmployeeImport start — rows={}, dryRun={}, includeResigned={}, createUsers={}",
                rows.size(), opts.isDryRun(), opts.isIncludeResigned(), opts.isCreateUsers());

        // Runtime-resolved policy values (read once per import, not per row)
        final String defaultPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, FALLBACK_DEFAULT_PASSWORD);
        final String defaultRoleName = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_ROLE, FALLBACK_DEFAULT_ROLE);
        final String defaultCountry = systemConfig.getOrDefault(
                SystemConfigService.Keys.IMPORT_EMPLOYEE_DEFAULT_COUNTRY, FALLBACK_DEFAULT_COUNTRY);
        final String defaultLocation = systemConfig.getOrDefault(
                SystemConfigService.Keys.IMPORT_EMPLOYEE_DEFAULT_LOCATION, FALLBACK_DEFAULT_LOCATION);

        // Each iteration builds a fresh TransactionTemplate-scoped unit of
        // work. Configure once, reuse across rows.
        final TransactionTemplate perRowTx = new TransactionTemplate(transactionManager);
        perRowTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Captured across rows so pass 2 can resolve manager references
        // after everything in pass 1 has been committed.
        final Map<String, String> pendingManagerByEmployeeCode = new HashMap<>();

        // -----------------------------------------------------------------
        // Pass 1 — upsert employees + user accounts + salary structure,
        // each in its own REQUIRES_NEW nested transaction.
        // -----------------------------------------------------------------
        for (EmployeeImportDTO dto : rows) {
            final Integer rowNum = dto.getRowNumber();
            final String rawCode = dto.getEmployeeCode();

            try {
                perRowTx.executeWithoutResult(status ->
                        processSingleRow(dto, opts, response,
                                defaultPassword, defaultRoleName, defaultCountry, defaultLocation,
                                pendingManagerByEmployeeCode));
            } catch (Exception e) {
                log.error("Row {} employee={} failed: {}", rowNum, rawCode, e.getMessage(), e);
                response.addFailure(rowNum, rawCode, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // -----------------------------------------------------------------
        // Pass 2 — resolve manager references in its own nested transaction
        // so a single bad reference doesn't poison the whole pass.
        // -----------------------------------------------------------------
        if (!opts.isDryRun() && !pendingManagerByEmployeeCode.isEmpty()) {
            try {
                perRowTx.executeWithoutResult(status ->
                        resolveManagerReferences(pendingManagerByEmployeeCode, response));
            } catch (Exception e) {
                log.error("Pass 2 manager resolution failed: {}", e.getMessage(), e);
                response.addWarning("Manager resolution pass failed: " + e.getMessage());
            }
        }

        log.info("EmployeeImport done — total={}, inserted={}, updated={}, skipped={}, failed={}",
                response.getTotalRecords(), response.getInsertedCount(), response.getUpdatedCount(),
                response.getSkippedCount(), response.getFailureCount());

        return response;
    }

    /**
     * Process a single row inside its caller's active transaction. Any
     * {@link RuntimeException} thrown here propagates out and the caller's
     * TransactionTemplate rolls back exactly this row's work, leaving the
     * next row to open a fresh session.
     * <p>
     * The deptCache is scoped per-row (not per-import) because Department
     * entities resolved in one nested transaction would be detached in the
     * next — reusing them would re-introduce the very session-poisoning
     * this refactor is trying to prevent.
     */
    private void processSingleRow(EmployeeImportDTO dto,
                                  Options opts,
                                  ImportResponse response,
                                  String defaultPassword,
                                  String defaultRoleName,
                                  String defaultCountry,
                                  String defaultLocation,
                                  Map<String, String> pendingManagerByEmployeeCode) {
        Integer rowNum = dto.getRowNumber();
        String rawCode = dto.getEmployeeCode();

        if (rawCode == null || rawCode.trim().isEmpty()) {
            response.addFailure(rowNum, null, "Employee code is required");
            return;
        }
        String employeeCode = rawCode.trim().toUpperCase();

        if (!opts.isIncludeResigned() && looksResigned(dto.getStatus())) {
            response.addSkipped(rowNum, employeeCode, null,
                    "Row skipped because status=" + dto.getStatus() + " and includeResigned=false");
            return;
        }

        Optional<Employee> existing = employeeRepository.findByEmployeeId(employeeCode);
        boolean isUpdate = existing.isPresent();

        Employee employee = isUpdate ? existing.get() : new Employee();
        if (!isUpdate) {
            employee.setEmployeeId(employeeCode);
        }

        // Dry-run + update: mutate a managed Employee purely for validation.
        // Detach first so the mutations never reach the DB at commit time.
        if (isUpdate && opts.isDryRun()) {
            entityManager.detach(employee);
        }

        // Fresh cache per row — Department entities from a prior row's
        // transaction are detached by the time the next tx opens.
        Map<String, Department> deptCache = new HashMap<>();

        applyRowToEmployee(employee, dto, deptCache, defaultCountry, defaultLocation, opts.isDryRun());

        if (!opts.isDryRun()) {
            employee = employeeRepository.save(employee);
        }

        // Salary structure — only if the row actually declares a CTC model.
        // Rows that omit all salary fields keep their existing structure (if any).
        if (dto.getStructureType() != null && !dto.getStructureType().trim().isEmpty()) {
            try {
                upsertSalaryStructure(employee, dto, opts.isDryRun());
            } catch (Exception salaryEx) {
                // Non-fatal: HR can fix the salary row separately. But if
                // the save attempt already corrupted this tx, flag it as a
                // warning and let the row tx still try to commit — Spring
                // will decide based on whether the tx was marked rollback-only.
                response.addWarning("Row " + rowNum + " (" + employeeCode + "): salary structure not applied — "
                        + salaryEx.getMessage());
                log.warn("Skipped salary structure for {} on row {}: {}", employeeCode, rowNum, salaryEx.getMessage());
            }
        }

        // User provisioning — always ROLE_EMPLOYEE. Admin/HR grants are a
        // manual step (AuthController.adminCreateUser).
        if (opts.isCreateUsers() && !opts.isDryRun()) {
            ensureUserAccount(employee, defaultPassword, defaultRoleName);
        }

        // Capture manager reference for pass 2
        String mgrCode = trimOrNull(dto.getReportingManagerCode());
        if (mgrCode != null) {
            pendingManagerByEmployeeCode.put(employeeCode, mgrCode.toUpperCase());
        }

        // Response counters are updated last so a rollback on the line
        // above this comment leaves the response accurate.
        if (isUpdate) {
            response.addUpdated(rowNum, employeeCode, employee.getFullName(),
                    opts.isDryRun() ? "Would update" : "Updated");
        } else {
            response.addInserted(rowNum, employeeCode, employee.getFullName(),
                    opts.isDryRun() ? "Would insert" : "Inserted");
        }
    }

    /**
     * Pass 2 body — extracted so it can be wrapped by the caller's own
     * TransactionTemplate callback.
     */
    private void resolveManagerReferences(Map<String, String> pendingManagerByEmployeeCode,
                                          ImportResponse response) {
        int resolved = 0;
        int selfRef = 0;
        int missing = 0;

        for (Map.Entry<String, String> entry : pendingManagerByEmployeeCode.entrySet()) {
            String empCode = entry.getKey();
            String mgrCode = entry.getValue();

            Optional<Employee> empOpt = employeeRepository.findByEmployeeId(empCode);
            if (empOpt.isEmpty()) {
                // The employee's own row must have rolled back in pass 1.
                continue;
            }
            Employee emp = empOpt.get();

            if (empCode.equalsIgnoreCase(mgrCode)) {
                emp.setManager(null);
                employeeRepository.save(emp);
                selfRef++;
                response.addWarning("Employee " + empCode + " listed itself as its own manager — manager field cleared");
                continue;
            }

            Optional<Employee> mgrOpt = employeeRepository.findByEmployeeId(mgrCode);
            if (mgrOpt.isEmpty()) {
                missing++;
                response.addWarning("Employee " + empCode + ": reporting manager " + mgrCode + " not found in this batch or existing data — manager not set");
                continue;
            }

            emp.setManager(mgrOpt.get());
            employeeRepository.save(emp);
            resolved++;
        }
        log.info("Manager pass 2 complete — resolved={}, selfReferences={}, missing={}",
                resolved, selfRef, missing);
    }

    // ------------------------------------------------------------------
    // Row → Employee mapping
    // ------------------------------------------------------------------

    private void applyRowToEmployee(Employee emp,
                                    EmployeeImportDTO dto,
                                    Map<String, Department> deptCache,
                                    String defaultCountry,
                                    String defaultLocation,
                                    boolean dryRun) {
        // -------------------- Identity --------------------
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()
                && isBlank(dto.getFirstName()) && isBlank(dto.getLastName())) {
            String[] parts = dto.getFullName().trim().split("\\s+", 2);
            emp.setFirstName(parts[0]);
            emp.setLastName(parts.length > 1 ? parts[1] : "");
        } else {
            if (!isBlank(dto.getFirstName())) emp.setFirstName(dto.getFirstName().trim());
            if (!isBlank(dto.getLastName()))  emp.setLastName(dto.getLastName().trim());
        }

        // Guarantee non-null/non-blank names (DB columns are NOT NULL).
        if (isBlank(emp.getFirstName())) emp.setFirstName(emp.getEmployeeId());
        if (emp.getLastName() == null)   emp.setLastName("");

        setIfPresent(dto.getMiddleName(),  emp::setMiddleName);
        setIfPresent(dto.getNickName(),    emp::setNickName);
        setIfPresent(dto.getFatherName(),  emp::setFatherName);
        setIfPresent(dto.getSpouseName(),  emp::setSpouseName);

        parseDate(dto.getDateOfBirth()).ifPresent(emp::setDateOfBirth);
        parseEnum(dto.getGender(), Gender.class, EmployeeImportServiceImpl::normalizeGender).ifPresent(emp::setGender);
        parseEnum(dto.getMaritalStatus(), MaritalStatus.class, String::toUpperCase).ifPresent(emp::setMaritalStatus);
        parseDate(dto.getMarriageDate()).ifPresent(emp::setMarriageDate);
        parseEnum(dto.getBloodGroup(), BloodGroup.class, EmployeeImportServiceImpl::normalizeBloodGroup).ifPresent(emp::setBloodGroup);

        // -------------------- Contact --------------------
        if (!isBlank(dto.getEmail())) {
            emp.setEmail(dto.getEmail().trim().toLowerCase());
        }
        setIfPresent(dto.getPersonalEmail(),   v -> emp.setPersonalEmail(v.trim().toLowerCase()));
        setIfPresent(dto.getOfficialEmail(),   v -> emp.setOfficialEmail(v.trim().toLowerCase()));
        setIfPresent(dto.getPhone(),           emp::setPhone);
        setIfPresent(dto.getMobileNumber(),    emp::setMobileNumber);
        setIfPresent(dto.getExtensionNumber(), emp::setExtensionNumber);
        setIfPresent(dto.getAddress(),         emp::setAddress);
        setIfPresent(dto.getCity(),            emp::setCity);
        setIfPresent(dto.getState(),           emp::setState);
        setIfPresent(dto.getPincode(),         emp::setPincode);
        setIfPresent(dto.getCountryOfOrigin(), emp::setCountryOfOrigin);
        if (emp.getCountryOfOrigin() == null) emp.setCountryOfOrigin(defaultCountry);
        setIfPresent(dto.getLocation(),        emp::setLocation);
        if (emp.getLocation() == null)         emp.setLocation(defaultLocation);
        parseBoolean(dto.getIsPhysicalChallenged()).ifPresent(emp::setIsPhysicalChallenged);
        parseBoolean(dto.getIsInternationalEmployee()).ifPresent(emp::setIsInternationalEmployee);

        // -------------------- Employment --------------------
        parseDate(dto.getDateOfJoining()).ifPresent(emp::setDateOfJoining);
        if (emp.getDateOfJoining() == null) {
            emp.setDateOfJoining(LocalDate.now());
        }
        parseDate(dto.getConfirmDate()).ifPresent(emp::setConfirmDate);
        parseDate(dto.getDateOfResignation()).ifPresent(emp::setDateOfResignation);
        parseDate(dto.getLastWorkingDate()).ifPresent(emp::setLastWorkingDate);

        if (!isBlank(dto.getDepartment())) {
            Department dept = resolveOrCreateDepartment(dto.getDepartment().trim(), deptCache, dryRun);
            // In dry-run mode resolveOrCreateDepartment returns null for
            // brand-new department names (see the comment in that method).
            // Leave the existing department assignment untouched in that
            // case — the validation report still tells HR that the
            // department would be auto-created on a real run.
            if (dept != null) {
                emp.setDepartment(dept);
            }
        }

        setIfPresent(dto.getDesignation(), emp::setDesignation);
        parseEnum(dto.getEmploymentType(), EmploymentType.class, EmployeeImportServiceImpl::normalizeEmploymentType)
                .ifPresent(emp::setEmploymentType);
        parseEnum(dto.getEmployeeCategory(), EmployeeCategory.class, EmployeeImportServiceImpl::normalizeEmployeeCategory)
                .ifPresent(emp::setEmployeeCategory);
        setIfPresent(dto.getEmployeeSeries(), emp::setEmployeeSeries);
        parseEnum(dto.getProducerType(), ProducerType.class, EmployeeImportServiceImpl::normalizeProducerType)
                .ifPresent(emp::setProducerType);
        setIfPresent(dto.getEmployeeReferenceNumber(), emp::setEmployeeReferenceNumber);
        setIfPresent(dto.getCostCenter(),   emp::setCostCenter);
        setIfPresent(dto.getDivision(),     emp::setDivision);
        setIfPresent(dto.getGrade(),        emp::setGrade);
        setIfPresent(dto.getTeamName(),     emp::setTeamName);
        setIfPresent(dto.getManagerNameText(), emp::setManagerNameText);
        setIfPresent(dto.getBranchHead(),   emp::setBranchHead);
        setIfPresent(dto.getUnitHead(),     emp::setUnitHead);
        parseInt(dto.getProbationPeriodDays()).ifPresent(emp::setProbationPeriodDays);
        parseInt(dto.getNoticePeriodDays()).ifPresent(emp::setNoticePeriodDays);
        parseEnum(dto.getStatus(), EmployeeStatus.class, EmployeeImportServiceImpl::normalizeEmployeeStatus)
                .ifPresent(emp::setStatus);
        if (emp.getStatus() == null) emp.setStatus(EmployeeStatus.ACTIVE);

        // -------------------- Device / login --------------------
        Integer devCode = null;
        if (!isBlank(dto.getEmpCodeOnDevice())) {
            try {
                devCode = Integer.parseInt(dto.getEmpCodeOnDevice().trim().replaceAll("\\D", ""));
            } catch (NumberFormatException ignored) {
                // fall through to derivation below
            }
        }
        if (devCode == null) {
            devCode = deriveDeviceCode(emp.getEmployeeId());
        }
        if (devCode != null) {
            emp.setEmpCodeOnDevice(devCode);
        }

        String loginUsername = !isBlank(dto.getLoginUsername())
                ? dto.getLoginUsername().trim().toLowerCase()
                : (emp.getEmployeeId() != null ? emp.getEmployeeId().toLowerCase() : null);
        if (loginUsername != null) {
            emp.setLoginUsername(loginUsername);
        }

        // -------------------- Emergency contact --------------------
        setIfPresent(dto.getEmergencyContactName(),         emp::setEmergencyContactName);
        setIfPresent(dto.getEmergencyContactPhone(),        emp::setEmergencyContactPhone);
        setIfPresent(dto.getEmergencyContactRelationship(), emp::setEmergencyContactRelationship);

        // -------------------- Background verification --------------------
        parseEnum(dto.getBackgroundCheckStatus(), BackgroundCheckStatus.class, EmployeeImportServiceImpl::normalizeBackgroundCheck)
                .ifPresent(emp::setBackgroundCheckStatus);
        parseDate(dto.getBackgroundVerificationDate()).ifPresent(emp::setBackgroundVerificationDate);
        setIfPresent(dto.getBackgroundAgencyName(),   emp::setBackgroundAgencyName);
        setIfPresent(dto.getBackgroundCheckRemarks(), emp::setBackgroundCheckRemarks);

        // -------------------- Banking --------------------
        setIfPresent(dto.getBankAccountNumber(), emp::setBankAccountNumber);
        setIfPresent(dto.getBankIfscCode(),      v -> emp.setBankIfscCode(v.toUpperCase()));
        setIfPresent(dto.getBankName(),          emp::setBankName);
        setIfPresent(dto.getBankAccountType(),   emp::setBankAccountType);
        setIfPresent(dto.getBankBranch(),        emp::setBankBranch);
        setIfPresent(dto.getSalaryPaymentMode(), emp::setSalaryPaymentMode);
        setIfPresent(dto.getDdPayableAt(),       emp::setDdPayableAt);
        setIfPresent(dto.getNameAsPerBank(),     emp::setNameAsPerBank);
        setIfPresent(dto.getIban(),              emp::setIban);

        // -------------------- Statutory --------------------
        setIfPresent(dto.getPanNumber(),          v -> emp.setPanNumber(v.toUpperCase()));
        setIfPresent(dto.getAadhaarNumber(),      v -> emp.setAadhaarNumber(v.replaceAll("\\s+", "")));
        setIfPresent(dto.getAadhaarEnrolmentNo(), emp::setAadhaarEnrolmentNo);
        setIfPresent(dto.getAadhaarName(),        emp::setAadhaarName);
        setIfPresent(dto.getUanNumber(),          emp::setUanNumber);
        parseBoolean(dto.getPfEligible()).ifPresent(emp::setPfEligible);
        setIfPresent(dto.getPfNumber(),           emp::setPfNumber);
        setIfPresent(dto.getPfScheme(),           emp::setPfScheme);
        parseDate(dto.getPfJoiningDate()).ifPresent(emp::setPfJoiningDate);
        parseBoolean(dto.getExcessEpfEligible()).ifPresent(emp::setExcessEpfEligible);
        parseBoolean(dto.getExcessEpsEligible()).ifPresent(emp::setExcessEpsEligible);
        parseBoolean(dto.getExistingPfMember()).ifPresent(emp::setExistingPfMember);
        parseBoolean(dto.getEsiEligible()).ifPresent(emp::setEsiEligible);
        setIfPresent(dto.getEsiNumber(), emp::setEsiNumber);
        parseBoolean(dto.getLwfEligible()).ifPresent(emp::setLwfEligible);

        // -------------------- Misc operational --------------------
        setIfPresent(dto.getTargetInfo(),        emp::setTargetInfo);
        setIfPresent(dto.getEmployeeRemarks(),   emp::setEmployeeRemarks);
        setIfPresent(dto.getOfferLetterIssued(), emp::setOfferLetterIssued);
        setIfPresent(dto.getIdCardStatus(),      emp::setIdCardStatus);
        setIfPresent(dto.getPunchingStatus(),    emp::setPunchingStatus);
    }

    // ------------------------------------------------------------------
    // Department resolution / creation
    // ------------------------------------------------------------------

    private Department resolveOrCreateDepartment(String name, Map<String, Department> cache, boolean dryRun) {
        String key = name.toLowerCase();
        if (cache.containsKey(key)) {
            // cache may legitimately hold null in dry-run — short-circuit here
            return cache.get(key);
        }

        Department found = departmentRepository.findByNameIgnoreCase(name).orElse(null);
        if (found == null) {
            if (dryRun) {
                // Dry-run contract: nothing persists. We CANNOT return a
                // transient Department stub here because attaching it to a
                // (potentially managed) Employee would poison the Hibernate
                // session — the next query would trigger an autoflush that
                // finds a dirty Employee pointing at a transient Department
                // and raise TransientPropertyValueException. That exception
                // propagates through the repository's @Transactional proxy,
                // which marks the outer transaction rollback-only, and every
                // subsequent row fails the same way → UnexpectedRollbackException
                // at commit. This is exactly the crash we just fixed.
                //
                // So in dry-run we return null, cache the null, log what a
                // real run would do, and let the caller simply skip the
                // department assignment. The row still validates; HR just
                // sees in the report that the department would be auto-created.
                log.info("Dry run — would auto-create department '{}' (code would derive from name)", name);
                cache.put(key, null);
                return null;
            }
            // Real run: saveAndFlush so the Department has an assigned ID
            // before it is attached to any Employee. This also means the
            // INSERT statement has already executed, so subsequent
            // autoflushes don't have anything transient to fail on.
            Department created = Department.builder()
                    .name(name)
                    .code(generateDepartmentCode(name))
                    .description("Auto-created from employee import")
                    .build();
            found = departmentRepository.saveAndFlush(created);
            log.info("Auto-created department '{}' with code {}", name, found.getCode());
        }
        cache.put(key, found);
        return found;
    }

    private String generateDepartmentCode(String name) {
        String base = name == null ? "DEPT" : name.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (base.length() > 5) base = base.substring(0, 5);
        if (base.isEmpty()) base = "DEPT";

        String candidate = base;
        int suffix = 1;
        while (departmentRepository.existsByCode(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
            if (suffix > 9999) { // defensive guard
                candidate = base + "-" + System.currentTimeMillis();
                break;
            }
        }
        return candidate;
    }

    // ------------------------------------------------------------------
    // User provisioning
    // ------------------------------------------------------------------

    private void ensureUserAccount(Employee employee, String defaultPassword, String defaultRoleName) {
        if (employee == null || employee.getId() == null) return;

        Optional<User> existing = userRepository.findByEmployeeId(employee.getId());
        String username = employee.getLoginUsername() != null
                ? employee.getLoginUsername()
                : employee.getEmployeeId().toLowerCase();

        if (existing.isPresent()) {
            User user = existing.get();
            // Don't clobber the username or password if the account already
            // exists — admins may have rotated either already. Ensure the
            // account is active and has the default role at minimum.
            user.setIsActive(Boolean.TRUE);
            ensureDefaultRole(user, defaultRoleName);
            userRepository.save(user);
            log.debug("Ensured user for employee {} — active=true, role={}", employee.getEmployeeId(), defaultRoleName);
            return;
        }

        // Username collision with a different employee — log and skip; this
        // should never happen in practice because usernames derive from the
        // unique employee code.
        if (userRepository.existsByUsername(username)) {
            log.warn("Username {} already taken by another user — skipping user creation for {}",
                    username, employee.getEmployeeId());
            return;
        }

        User user = User.builder()
                .employee(employee)
                .username(username)
                .passwordHash(passwordEncoder.encode(defaultPassword))
                .isActive(Boolean.TRUE)
                .build();
        ensureDefaultRole(user, defaultRoleName);
        userRepository.save(user);
        log.debug("Created user {} (employee {}) with role {}", username, employee.getEmployeeId(), defaultRoleName);
    }

    private void ensureDefaultRole(User user, String roleName) {
        RoleType target;
        try {
            target = RoleType.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            log.warn("Configured default role '{}' is not a valid RoleType; falling back to ROLE_EMPLOYEE", roleName);
            target = RoleType.ROLE_EMPLOYEE;
        }
        final RoleType effective = target;
        boolean alreadyHas = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == effective);
        if (alreadyHas) return;

        Role role = roleRepository.findByName(effective).orElseThrow(
                () -> new IllegalStateException("Role " + effective + " is missing — check Flyway seed data"));
        user.addRole(role);
    }

    // ------------------------------------------------------------------
    // Salary structure upsert
    // ------------------------------------------------------------------

    private void upsertSalaryStructure(Employee employee, EmployeeImportDTO dto, boolean dryRun) {
        if (dryRun || employee.getId() == null) return;

        SalaryStructureType type;
        try {
            type = SalaryStructureType.valueOf(dto.getStructureType().trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown structureType: " + dto.getStructureType());
        }

        SalaryStructure ss = salaryStructureRepository.findByEmployeeId(employee.getId())
                .orElseGet(SalaryStructure::new);

        ss.setEmployee(employee);
        ss.setStructureType(type);
        parseBigDecimal(dto.getMonthlyGrossCtc()).ifPresent(ss::setMonthlyGrossCtc);
        parseBigDecimal(dto.getNth()).ifPresent(ss::setNth);
        parseBigDecimal(dto.getAnnualCtc()).ifPresent(ss::setAnnualCtc);
        parseBigDecimal(dto.getTdsAmount()).ifPresent(ss::setTdsAmount);
        parseBigDecimal(dto.getTdsRatePercent()).ifPresent(ss::setTdsRatePercent);
        parseBigDecimal(dto.getEmployerPf()).ifPresent(ss::setEmployerPf);
        parseBigDecimal(dto.getEmployeePf()).ifPresent(ss::setEmployeePf);
        parseBigDecimal(dto.getEmployerEsi()).ifPresent(ss::setEmployerEsi);
        parseBigDecimal(dto.getEmployeeEsi()).ifPresent(ss::setEmployeeEsi);
        parseBigDecimal(dto.getLwfAmount()).ifPresent(ss::setLwfAmount);
        parseBigDecimal(dto.getIncentives()).ifPresent(ss::setIncentives);
        parseBigDecimal(dto.getOtherDeductions()).ifPresent(ss::setOtherDeductions);
        parseInt(dto.getNumOfMonths()).ifPresent(ss::setNumOfMonths);

        if (ss.getEffectiveFrom() == null) {
            ss.setEffectiveFrom(employee.getDateOfJoining() != null ? employee.getDateOfJoining() : LocalDate.now());
        }
        if (ss.getIsActive() == null) {
            ss.setIsActive(Boolean.TRUE);
        }

        // CTC model: no sensible defaults for monthlyGrossCtc — a row that
        // declared structureType but no gross is a data error.
        if (ss.getMonthlyGrossCtc() == null) {
            throw new IllegalArgumentException("structureType present but monthlyGrossCtc missing");
        }

        salaryStructureRepository.save(ss);
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void setIfPresent(String raw, java.util.function.Consumer<String> setter) {
        if (!isBlank(raw)) setter.accept(raw.trim());
    }

    private static Optional<LocalDate> parseDate(String raw) {
        if (isBlank(raw)) return Optional.empty();
        String s = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(s, fmt));
            } catch (DateTimeParseException ignored) {
                // keep trying
            }
        }
        // Try ISO-local-date-time (drop the time portion)
        try {
            return Optional.of(LocalDate.parse(s.substring(0, Math.min(10, s.length()))));
        } catch (Exception ignored) {
            log.debug("Could not parse date: {}", raw);
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> parseBigDecimal(String raw) {
        if (isBlank(raw)) return Optional.empty();
        try {
            String cleaned = raw.trim().replace(",", "").replaceAll("[^0-9.\\-]", "");
            if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) return Optional.empty();
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String raw) {
        if (isBlank(raw)) return Optional.empty();
        try {
            String cleaned = raw.trim().replaceAll("[^0-9\\-]", "");
            if (cleaned.isEmpty() || cleaned.equals("-")) return Optional.empty();
            return Optional.of(Integer.parseInt(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> parseBoolean(String raw) {
        if (isBlank(raw)) return Optional.empty();
        String v = raw.trim().toLowerCase();
        if (v.equals("y") || v.equals("yes") || v.equals("true") || v.equals("1") || v.equals("eligible")) {
            return Optional.of(Boolean.TRUE);
        }
        if (v.equals("n") || v.equals("no") || v.equals("false") || v.equals("0") || v.equals("not eligible")) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    /** Generic enum parser with a normaliser that converts the raw value to the enum's canonical name. */
    private static <E extends Enum<E>> Optional<E> parseEnum(String raw, Class<E> type,
                                                             java.util.function.Function<String, String> normaliser) {
        if (isBlank(raw)) return Optional.empty();
        String normalised = normaliser.apply(raw.trim());
        if (normalised == null) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf(type, normalised));
        } catch (IllegalArgumentException e) {
            log.debug("Could not parse {} value: {} (normalised={})", type.getSimpleName(), raw, normalised);
            return Optional.empty();
        }
    }

    // -- normalisers ----------------------------------------------------

    private static String normalizeGender(String v) {
        String upper = v.toUpperCase();
        if (upper.startsWith("M"))  return "MALE";
        if (upper.startsWith("F"))  return "FEMALE";
        if (upper.contains("NOT"))  return "PREFER_NOT_TO_SAY";
        if (upper.startsWith("O"))  return "OTHER";
        return upper;
    }

    private static String normalizeBloodGroup(String v) {
        String s = v.toUpperCase().replaceAll("\\s+", "");
        // accept A+, A-, O+, etc.
        String sign = null;
        if (s.endsWith("+") || s.endsWith("POSITIVE") || s.endsWith("POS")) sign = "POSITIVE";
        else if (s.endsWith("-") || s.endsWith("NEGATIVE") || s.endsWith("NEG")) sign = "NEGATIVE";
        String letters = s.replaceAll("[^A-Z]", "").replace("POSITIVE", "").replace("NEGATIVE", "")
                .replace("POS", "").replace("NEG", "");
        if (sign == null || letters.isEmpty()) return "UNKNOWN";
        return letters + "_" + sign;
    }

    private static String normalizeEmploymentType(String v) {
        String upper = v.toUpperCase().trim();
        if (upper.contains("PART"))       return "PART_TIME";
        if (upper.contains("CONTRACT"))   return "CONTRACT";
        if (upper.contains("INTERN"))     return "INTERN";
        if (upper.contains("PROB"))       return "PROBATION";
        return "FULL_TIME";
    }

    private static String normalizeEmployeeCategory(String v) {
        String upper = v.toUpperCase().trim();
        if (upper.contains("CONTRACT"))  return "CONTRACT_EMPLOYEE";
        if (upper.contains("INTERN"))    return "INTERN";
        if (upper.contains("CONSULT"))   return "CONSULTANT";
        return "PERMANENT";
    }

    private static String normalizeProducerType(String v) {
        return v.toUpperCase().trim().contains("NON") ? "NON_PRODUCER" : "PRODUCER";
    }

    private static String normalizeEmployeeStatus(String v) {
        String upper = v.toUpperCase().trim();
        if (upper.contains("TERMINAT"))                          return "TERMINATED";
        if (upper.contains("NOTICE"))                            return "ON_NOTICE";
        if (upper.contains("SUSPEND"))                           return "SUSPENDED";
        if (upper.contains("RESIGN") || upper.contains("INACT")) return "INACTIVE";
        return "ACTIVE";
    }

    private static String normalizeBackgroundCheck(String v) {
        String upper = v.toUpperCase().trim();
        if (upper.contains("PROGRESS"))     return "IN_PROGRESS";
        if (upper.contains("COMPLET"))      return "COMPLETED";
        if (upper.contains("FAIL"))         return "FAILED";
        if (upper.contains("NOT APPLIC") || upper.contains("N/A") || upper.equals("NA")) return "NOT_APPLICABLE";
        return "NOT_INITIATED";
    }

    /** RESIGNED rows are distinguishable from the status column only. */
    private static boolean looksResigned(String status) {
        if (status == null) return false;
        String upper = status.trim().toUpperCase();
        return upper.contains("RESIGN") || upper.contains("INACTIVE") || upper.contains("TERMINAT")
                || upper.equals("INACTIVE");
    }

    /** Biometric device code = last 5 digits of the numeric portion of the employee code. */
    private static Integer deriveDeviceCode(String employeeId) {
        if (employeeId == null) return null;
        String digits = employeeId.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        String last5 = digits.length() > 5 ? digits.substring(digits.length() - 5) : digits;
        try {
            return Integer.parseInt(last5);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
