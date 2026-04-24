package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.imports.EmployeeImportDTO;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.EmployeeImportService;
import com.financebuddha.finbud.hrms.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thin adapter: parses an uploaded spreadsheet into
 * {@link EmployeeImportDTO} rows and hands off to {@link EmployeeImportService}
 * for the actual upsert + user provisioning work. All business logic lives
 * in the delegate; this class only knows about POI and header normalisation.
 * <p>
 * The previous iteration of this class contained hard-coded role assignment
 * (ADMIN_CODE="ND33004", HR_CODE="ND33301") and a baked-in default password,
 * which were removed in Checkpoint 5. Role upgrades now go through
 * {@code /api/auth/admin/create-user} and passwords come from
 * {@link com.financebuddha.finbud.hrms.service.SystemConfigService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final EmployeeImportService employeeImportService;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    /**
     * Employee codes whose rows are protected from {@link #cleanupEmployeeData}.
     * The Finbud Noida master originally baked in ND33004 (admin) and ND33301
     * (HR). We keep those reserved here as a belt-and-braces guard even though
     * role assignment itself no longer keys off them.
     */
    private static final Set<String> RESERVED_EMPLOYEE_CODES = new HashSet<>(Arrays.asList(
            "ND33004", "ND33301"
    ));

    private static final Map<String, String> HEADER_MAPPINGS = new HashMap<>();

    static {
        // Identity
        HEADER_MAPPINGS.put("emp code", "employeeCode");
        HEADER_MAPPINGS.put("employee code", "employeeCode");
        HEADER_MAPPINGS.put("empcode", "employeeCode");
        HEADER_MAPPINGS.put("code", "employeeCode");
        HEADER_MAPPINGS.put("employee id", "employeeCode");
        HEADER_MAPPINGS.put("emp id", "employeeCode");
        HEADER_MAPPINGS.put("id", "employeeCode");

        HEADER_MAPPINGS.put("first name", "firstName");
        HEADER_MAPPINGS.put("firstname", "firstName");
        HEADER_MAPPINGS.put("first", "firstName");
        HEADER_MAPPINGS.put("middle name", "middleName");
        HEADER_MAPPINGS.put("last name", "lastName");
        HEADER_MAPPINGS.put("lastname", "lastName");
        HEADER_MAPPINGS.put("last", "lastName");
        HEADER_MAPPINGS.put("full name", "fullName");
        HEADER_MAPPINGS.put("name", "fullName");
        HEADER_MAPPINGS.put("employee name", "fullName");
        HEADER_MAPPINGS.put("nick name", "nickName");
        HEADER_MAPPINGS.put("father name", "fatherName");
        HEADER_MAPPINGS.put("spouse name", "spouseName");
        HEADER_MAPPINGS.put("date of birth", "dateOfBirth");
        HEADER_MAPPINGS.put("dob", "dateOfBirth");
        HEADER_MAPPINGS.put("gender", "gender");
        HEADER_MAPPINGS.put("marital status", "maritalStatus");
        HEADER_MAPPINGS.put("marriage date", "marriageDate");
        HEADER_MAPPINGS.put("blood group", "bloodGroup");

        // Contact
        HEADER_MAPPINGS.put("email", "email");
        HEADER_MAPPINGS.put("email id", "email");
        HEADER_MAPPINGS.put("e-mail", "email");
        HEADER_MAPPINGS.put("personal email", "personalEmail");
        HEADER_MAPPINGS.put("official email", "officialEmail");
        HEADER_MAPPINGS.put("phone", "phone");
        HEADER_MAPPINGS.put("phone number", "phone");
        HEADER_MAPPINGS.put("mobile", "mobileNumber");
        HEADER_MAPPINGS.put("mobile no", "mobileNumber");
        HEADER_MAPPINGS.put("mobile number", "mobileNumber");
        HEADER_MAPPINGS.put("contact", "phone");
        HEADER_MAPPINGS.put("extension", "extensionNumber");
        HEADER_MAPPINGS.put("address", "address");
        HEADER_MAPPINGS.put("city", "city");
        HEADER_MAPPINGS.put("state", "state");
        HEADER_MAPPINGS.put("pincode", "pincode");
        HEADER_MAPPINGS.put("pin code", "pincode");
        HEADER_MAPPINGS.put("postal code", "pincode");
        HEADER_MAPPINGS.put("zip", "pincode");
        HEADER_MAPPINGS.put("zip code", "pincode");
        HEADER_MAPPINGS.put("country", "countryOfOrigin");
        HEADER_MAPPINGS.put("country of origin", "countryOfOrigin");
        HEADER_MAPPINGS.put("location", "location");

        // Employment
        HEADER_MAPPINGS.put("designation", "designation");
        HEADER_MAPPINGS.put("title", "designation");
        HEADER_MAPPINGS.put("job title", "designation");
        HEADER_MAPPINGS.put("role", "designation");
        HEADER_MAPPINGS.put("position", "designation");
        HEADER_MAPPINGS.put("department", "department");
        HEADER_MAPPINGS.put("dept", "department");
        HEADER_MAPPINGS.put("dept name", "department");
        HEADER_MAPPINGS.put("employment type", "employmentType");
        HEADER_MAPPINGS.put("emp type", "employmentType");
        HEADER_MAPPINGS.put("employee category", "employeeCategory");
        HEADER_MAPPINGS.put("category", "employeeCategory");
        HEADER_MAPPINGS.put("employee series", "employeeSeries");
        HEADER_MAPPINGS.put("producer type", "producerType");
        HEADER_MAPPINGS.put("employee reference number", "employeeReferenceNumber");
        HEADER_MAPPINGS.put("cost center", "costCenter");
        HEADER_MAPPINGS.put("division", "division");
        HEADER_MAPPINGS.put("grade", "grade");
        HEADER_MAPPINGS.put("team", "teamName");
        HEADER_MAPPINGS.put("team name", "teamName");
        HEADER_MAPPINGS.put("branch head", "branchHead");
        HEADER_MAPPINGS.put("unit head", "unitHead");
        HEADER_MAPPINGS.put("probation period", "probationPeriodDays");
        HEADER_MAPPINGS.put("notice period", "noticePeriodDays");
        HEADER_MAPPINGS.put("date of joining", "dateOfJoining");
        HEADER_MAPPINGS.put("doj", "dateOfJoining");
        HEADER_MAPPINGS.put("joining date", "dateOfJoining");
        HEADER_MAPPINGS.put("join date", "dateOfJoining");
        HEADER_MAPPINGS.put("confirm date", "confirmDate");
        HEADER_MAPPINGS.put("confirmation date", "confirmDate");
        HEADER_MAPPINGS.put("date of resignation", "dateOfResignation");
        HEADER_MAPPINGS.put("resignation date", "dateOfResignation");
        HEADER_MAPPINGS.put("last working date", "lastWorkingDate");
        HEADER_MAPPINGS.put("lwd", "lastWorkingDate");
        HEADER_MAPPINGS.put("status", "status");

        // Reporting
        HEADER_MAPPINGS.put("reporting manager", "reportingManagerCode");
        HEADER_MAPPINGS.put("reporting manager code", "reportingManagerCode");
        HEADER_MAPPINGS.put("manager code", "reportingManagerCode");
        HEADER_MAPPINGS.put("manager", "reportingManagerCode");
        HEADER_MAPPINGS.put("team leader", "reportingManagerCode");
        HEADER_MAPPINGS.put("supervisor", "reportingManagerCode");
        HEADER_MAPPINGS.put("manager id", "reportingManagerCode");
        HEADER_MAPPINGS.put("manager name", "managerNameText");

        // Device / login
        HEADER_MAPPINGS.put("emp code on device", "empCodeOnDevice");
        HEADER_MAPPINGS.put("device code", "empCodeOnDevice");
        HEADER_MAPPINGS.put("biometric code", "empCodeOnDevice");
        HEADER_MAPPINGS.put("login username", "loginUsername");

        // Emergency
        HEADER_MAPPINGS.put("emergency contact name", "emergencyContactName");
        HEADER_MAPPINGS.put("emergency name", "emergencyContactName");
        HEADER_MAPPINGS.put("emergency contact", "emergencyContactName");
        HEADER_MAPPINGS.put("emergency phone", "emergencyContactPhone");
        HEADER_MAPPINGS.put("emergency mobile", "emergencyContactPhone");
        HEADER_MAPPINGS.put("emergency contact no", "emergencyContactPhone");
        HEADER_MAPPINGS.put("emergency relation", "emergencyContactRelationship");
        HEADER_MAPPINGS.put("relationship", "emergencyContactRelationship");

        // Background verification
        HEADER_MAPPINGS.put("background check status", "backgroundCheckStatus");
        HEADER_MAPPINGS.put("bgv status", "backgroundCheckStatus");
        HEADER_MAPPINGS.put("background verification date", "backgroundVerificationDate");
        HEADER_MAPPINGS.put("bgv date", "backgroundVerificationDate");
        HEADER_MAPPINGS.put("background agency", "backgroundAgencyName");
        HEADER_MAPPINGS.put("bgv remarks", "backgroundCheckRemarks");

        // Banking
        HEADER_MAPPINGS.put("bank account", "bankAccountNumber");
        HEADER_MAPPINGS.put("account number", "bankAccountNumber");
        HEADER_MAPPINGS.put("bank a/c", "bankAccountNumber");
        HEADER_MAPPINGS.put("ifsc", "bankIfscCode");
        HEADER_MAPPINGS.put("ifsc code", "bankIfscCode");
        HEADER_MAPPINGS.put("bank ifsc", "bankIfscCode");
        HEADER_MAPPINGS.put("bank name", "bankName");
        HEADER_MAPPINGS.put("bank", "bankName");
        HEADER_MAPPINGS.put("account type", "bankAccountType");
        HEADER_MAPPINGS.put("bank branch", "bankBranch");
        HEADER_MAPPINGS.put("salary payment mode", "salaryPaymentMode");
        HEADER_MAPPINGS.put("dd payable at", "ddPayableAt");
        HEADER_MAPPINGS.put("name as per bank", "nameAsPerBank");
        HEADER_MAPPINGS.put("iban", "iban");

        // Statutory
        HEADER_MAPPINGS.put("pan", "panNumber");
        HEADER_MAPPINGS.put("pan number", "panNumber");
        HEADER_MAPPINGS.put("pan no", "panNumber");
        HEADER_MAPPINGS.put("aadhaar", "aadhaarNumber");
        HEADER_MAPPINGS.put("aadhar", "aadhaarNumber");
        HEADER_MAPPINGS.put("aadhaar number", "aadhaarNumber");
        HEADER_MAPPINGS.put("aadhaar enrolment no", "aadhaarEnrolmentNo");
        HEADER_MAPPINGS.put("aadhaar name", "aadhaarName");
        HEADER_MAPPINGS.put("uan", "uanNumber");
        HEADER_MAPPINGS.put("uan number", "uanNumber");
        HEADER_MAPPINGS.put("pf eligible", "pfEligible");
        HEADER_MAPPINGS.put("pf number", "pfNumber");
        HEADER_MAPPINGS.put("pf scheme", "pfScheme");
        HEADER_MAPPINGS.put("pf joining date", "pfJoiningDate");
        HEADER_MAPPINGS.put("excess epf eligible", "excessEpfEligible");
        HEADER_MAPPINGS.put("excess eps eligible", "excessEpsEligible");
        HEADER_MAPPINGS.put("existing pf member", "existingPfMember");
        HEADER_MAPPINGS.put("esi eligible", "esiEligible");
        HEADER_MAPPINGS.put("esi number", "esiNumber");
        HEADER_MAPPINGS.put("lwf eligible", "lwfEligible");

        // Salary (CTC model)
        HEADER_MAPPINGS.put("structure type", "structureType");
        HEADER_MAPPINGS.put("salary structure", "structureType");
        HEADER_MAPPINGS.put("monthly gross ctc", "monthlyGrossCtc");
        HEADER_MAPPINGS.put("monthly gross", "monthlyGrossCtc");
        HEADER_MAPPINGS.put("gross ctc", "monthlyGrossCtc");
        HEADER_MAPPINGS.put("nth", "nth");
        HEADER_MAPPINGS.put("net take home", "nth");
        HEADER_MAPPINGS.put("annual ctc", "annualCtc");
        HEADER_MAPPINGS.put("annual salary", "annualCtc");
        HEADER_MAPPINGS.put("ctc", "annualCtc");
        HEADER_MAPPINGS.put("tds", "tdsAmount");
        HEADER_MAPPINGS.put("tds amount", "tdsAmount");
        HEADER_MAPPINGS.put("tds rate", "tdsRatePercent");
        HEADER_MAPPINGS.put("tds rate percent", "tdsRatePercent");
        HEADER_MAPPINGS.put("employer pf", "employerPf");
        HEADER_MAPPINGS.put("employee pf", "employeePf");
        HEADER_MAPPINGS.put("employer esi", "employerEsi");
        HEADER_MAPPINGS.put("employee esi", "employeeEsi");
        HEADER_MAPPINGS.put("lwf", "lwfAmount");
        HEADER_MAPPINGS.put("lwf amount", "lwfAmount");
        HEADER_MAPPINGS.put("incentives", "incentives");
        HEADER_MAPPINGS.put("other deductions", "otherDeductions");
        HEADER_MAPPINGS.put("num of months", "numOfMonths");

        // Misc
        HEADER_MAPPINGS.put("target info", "targetInfo");
        HEADER_MAPPINGS.put("remarks", "employeeRemarks");
        HEADER_MAPPINGS.put("offer letter issued", "offerLetterIssued");
        HEADER_MAPPINGS.put("id card status", "idCardStatus");
        HEADER_MAPPINGS.put("punching status", "punchingStatus");

        // ------------------------------------------------------------------
        // Finbud Noida master sheet — exact headers observed in the
        // operations team's export. These overrides are written AFTER the
        // generic keys above so they win when a header normalises to the
        // same lowercase string via HashMap.put(). They also provide longer
        // keys than the generic ones, which wins the longest-match contest
        // in normalizeHeader() below.
        // ------------------------------------------------------------------
        HEADER_MAPPINGS.put("employee number",                     "employeeCode");
        HEADER_MAPPINGS.put("emp code on device",                  "empCodeOnDevice");
        HEADER_MAPPINGS.put("login user name",                     "loginUsername");

        // Name columns that must NOT degrade to "name" → fullName
        HEADER_MAPPINGS.put("father's name",                       "fatherName");
        HEADER_MAPPINGS.put("fathers name",                        "fatherName");
        HEADER_MAPPINGS.put("spousename",                          "spouseName");
        HEADER_MAPPINGS.put("spouse's name",                       "spouseName");
        HEADER_MAPPINGS.put("name (as on aadhaar card)",           "aadhaarName");
        HEADER_MAPPINGS.put("agency name",                         "backgroundAgencyName");
        HEADER_MAPPINGS.put("name as per bank records",            "nameAsPerBank");

        // Reporting — disambiguate manager CODE (col 23) vs manager NAME (col 73).
        // longest-match rule: "manager's employee no" (21) wins over "manager" (7).
        HEADER_MAPPINGS.put("manager's employee no",               "reportingManagerCode");
        HEADER_MAPPINGS.put("managers employee no",                "reportingManagerCode");
        HEADER_MAPPINGS.put("manager",                             "managerNameText"); // override earlier mapping
        HEADER_MAPPINGS.put("bh",                                  "branchHead");

        // Background verification — Finbud sheet uses verbose headers
        HEADER_MAPPINGS.put("background verification completed on","backgroundVerificationDate");

        // Statutory — Yes/No question-style headers
        HEADER_MAPPINGS.put("is employee eligible for pf?",                         "pfEligible");
        HEADER_MAPPINGS.put("is employee eligible for pf",                          "pfEligible");
        HEADER_MAPPINGS.put("is employee eligible for excess epf contribution?",    "excessEpfEligible");
        HEADER_MAPPINGS.put("is employee eligible for excess epf contribution",     "excessEpfEligible");
        HEADER_MAPPINGS.put("is employee eligible for excess eps contribution?",    "excessEpsEligible");
        HEADER_MAPPINGS.put("is employee eligible for excess eps contribution",     "excessEpsEligible");
        HEADER_MAPPINGS.put("is existing member of pf?",                            "existingPfMember");
        HEADER_MAPPINGS.put("is existing member of pf",                             "existingPfMember");
        HEADER_MAPPINGS.put("is employee eligible for esi?",                        "esiEligible");
        HEADER_MAPPINGS.put("is employee eligible for esi",                         "esiEligible");
        HEADER_MAPPINGS.put("is employee covered under lwf?",                       "lwfEligible");
        HEADER_MAPPINGS.put("is employee covered under lwf",                        "lwfEligible");

        // Aadhaar / UAN — "id" and "account number" generic keys otherwise win
        HEADER_MAPPINGS.put("aadhaar card number",                 "aadhaarNumber");
        HEADER_MAPPINGS.put("aadhaar card enrolment no",           "aadhaarEnrolmentNo");
        HEADER_MAPPINGS.put("universal account number",            "uanNumber");

        // Employee-series / producer variants used in the master sheet
        HEADER_MAPPINGS.put("employee number series",              "employeeSeries");
        HEADER_MAPPINGS.put("producer / non producer",             "producerType");

        // Status / operational — plain one-word headers that otherwise match the wrong keys
        HEADER_MAPPINGS.put("target",                              "targetInfo");
        HEADER_MAPPINGS.put("punching",                            "punchingStatus");
        HEADER_MAPPINGS.put("id card",                             "idCardStatus");
        HEADER_MAPPINGS.put("employee status",                     "status");
        HEADER_MAPPINGS.put("employee remarks",                    "employeeRemarks");
    }

    @Override
    public ImportResponse importEmployees(MultipartFile file) throws Exception {
        return importEmployees(file, EmployeeImportService.Options.builder().build());
    }

    @Override
    public ImportResponse importEmployees(MultipartFile file, EmployeeImportService.Options options) throws Exception {
        validateSpreadsheet(file);
        List<EmployeeImportDTO> rows = parseExcel(file);
        if (rows.isEmpty()) {
            throw new BadRequestException("No valid employee data found in the file");
        }
        EmployeeImportService.Options effective = options != null
                ? options
                : EmployeeImportService.Options.builder().build();
        log.info("Excel import — file={} rows={} dryRun={} includeResigned={} createUsers={}",
                file.getOriginalFilename(), rows.size(),
                effective.isDryRun(), effective.isIncludeResigned(), effective.isCreateUsers());
        return employeeImportService.importEmployees(rows, effective);
    }

    @Override
    public List<EmployeeImportDTO> parseExcel(MultipartFile file) throws Exception {
        validateSpreadsheet(file);
        List<EmployeeImportDTO> employees = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                log.info("Processing sheet: {} with {} rows", sheetName, sheet.getLastRowNum());

                if (sheet.getLastRowNum() < 1) continue;

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                Map<Integer, String> headerMap = createHeaderMap(headerRow);
                if (headerMap.isEmpty()) {
                    log.warn("No valid headers found in sheet: {}", sheetName);
                    continue;
                }

                for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row == null || isRowEmpty(row)) continue;

                    try {
                        EmployeeImportDTO dto = parseRow(row, headerMap, rowNum + 1);
                        if (dto != null && isValidEmployee(dto)) {
                            employees.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing row {} in sheet {}: {}", rowNum + 1, sheetName, e.getMessage());
                    }
                }
            }
        }

        log.info("Parsed {} employees from file", employees.size());
        return employees;
    }

    @Override
    @Transactional
    public void cleanupEmployeeData(boolean preserveSystemUsers) {
        log.info("Starting employee data cleanup. Preserve system users: {}", preserveSystemUsers);

        if (preserveSystemUsers) {
            // Keep rows linked to an active User account AND reserved codes.
            Set<Long> systemEmployeeIds = new HashSet<>();
            for (User user : userRepository.findAll()) {
                if (user.getEmployee() != null) {
                    systemEmployeeIds.add(user.getEmployee().getId());
                }
            }
            List<Employee> toDelete = employeeRepository.findAll().stream()
                    .filter(emp -> !systemEmployeeIds.contains(emp.getId()))
                    .filter(emp -> emp.getEmployeeId() == null
                            || !RESERVED_EMPLOYEE_CODES.contains(emp.getEmployeeId().toUpperCase()))
                    .toList();
            for (Employee emp : toDelete) {
                employeeRepository.delete(emp);
            }
            log.info("Deleted {} non-system employees", toDelete.size());
        } else {
            List<Employee> toDelete = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getEmployeeId() == null
                            || !RESERVED_EMPLOYEE_CODES.contains(emp.getEmployeeId().toUpperCase()))
                    .toList();
            for (Employee emp : toDelete) {
                employeeRepository.delete(emp);
            }
            log.info("Deleted {} employees (all except reserved codes)", toDelete.size());
        }
    }

    // ------------------------------------------------------------------
    // Parsing — POI specifics
    // ------------------------------------------------------------------

    private void validateSpreadsheet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or null");
        }
        String filename = file.getOriginalFilename();
        if (filename == null
                || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new BadRequestException("File must be .xlsx or .xls format");
        }
    }

    private Map<Integer, String> createHeaderMap(Row headerRow) {
        Map<Integer, String> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String header = getCellStringValue(cell);
            if (header == null || header.trim().isEmpty()) continue;

            String normalized = normalizeHeader(header.trim());
            if (normalized != null) {
                headerMap.put(i, normalized);
                log.trace("Mapped column {}: '{}' -> {}", i, header, normalized);
            }
        }
        log.info("Found {} mapped headers", headerMap.size());
        return headerMap;
    }

    private String normalizeHeader(String header) {
        String lower = header.toLowerCase().trim();
        String normalized = HEADER_MAPPINGS.get(lower);
        if (normalized != null) return normalized;

        // Try exact-match against any of the map keys contained in the header.
        // Prefer longer matches so "employee category" doesn't degrade to "employee".
        String bestKey = null;
        int bestLen = 0;
        for (Map.Entry<String, String> entry : HEADER_MAPPINGS.entrySet()) {
            if (lower.contains(entry.getKey()) && entry.getKey().length() > bestLen) {
                bestKey = entry.getKey();
                bestLen = entry.getKey().length();
            }
        }
        return bestKey == null ? null : HEADER_MAPPINGS.get(bestKey);
    }

    private EmployeeImportDTO parseRow(Row row, Map<Integer, String> headerMap, int rowNumber) {
        EmployeeImportDTO dto = EmployeeImportDTO.builder().rowNumber(rowNumber).build();
        StringBuilder rawData = new StringBuilder();

        for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
            int colIndex = entry.getKey();
            String fieldName = entry.getValue();
            Cell cell = row.getCell(colIndex);
            String value = getCellStringValue(cell);

            if (value != null && !value.isEmpty()) {
                rawData.append(fieldName).append(": ").append(value).append("; ");
                assignDtoField(dto, fieldName, value);
            }
        }

        dto.setRawData(rawData.toString());
        return dto;
    }

    /**
     * Field name → setter dispatch. Listed in the same order as
     * {@link EmployeeImportDTO} for easier cross-reference. Unknown
     * field names (shouldn't happen given the header map is the source
     * of truth) are silently ignored.
     */
    private void assignDtoField(EmployeeImportDTO dto, String fieldName, String value) {
        switch (fieldName) {
            // Identity
            case "employeeCode":      dto.setEmployeeCode(value); break;
            case "firstName":         dto.setFirstName(value); break;
            case "middleName":        dto.setMiddleName(value); break;
            case "lastName":          dto.setLastName(value); break;
            case "fullName":          dto.setFullName(value); break;
            case "nickName":          dto.setNickName(value); break;
            case "fatherName":        dto.setFatherName(value); break;
            case "spouseName":        dto.setSpouseName(value); break;
            case "dateOfBirth":       dto.setDateOfBirth(value); break;
            case "gender":            dto.setGender(value); break;
            case "maritalStatus":     dto.setMaritalStatus(value); break;
            case "marriageDate":      dto.setMarriageDate(value); break;
            case "bloodGroup":        dto.setBloodGroup(value); break;

            // Contact
            case "email":             dto.setEmail(value); break;
            case "personalEmail":     dto.setPersonalEmail(value); break;
            case "officialEmail":     dto.setOfficialEmail(value); break;
            case "phone":             dto.setPhone(value); break;
            case "mobileNumber":      dto.setMobileNumber(value); break;
            case "extensionNumber":   dto.setExtensionNumber(value); break;
            case "address":           dto.setAddress(value); break;
            case "city":              dto.setCity(value); break;
            case "state":             dto.setState(value); break;
            case "pincode":           dto.setPincode(value); break;
            case "countryOfOrigin":   dto.setCountryOfOrigin(value); break;
            case "location":          dto.setLocation(value); break;

            // Employment
            case "dateOfJoining":         dto.setDateOfJoining(value); break;
            case "confirmDate":           dto.setConfirmDate(value); break;
            case "dateOfResignation":     dto.setDateOfResignation(value); break;
            case "lastWorkingDate":       dto.setLastWorkingDate(value); break;
            case "department":            dto.setDepartment(value); break;
            case "designation":           dto.setDesignation(value); break;
            case "reportingManagerCode":  dto.setReportingManagerCode(value); break;
            case "managerNameText":       dto.setManagerNameText(value); break;
            case "employmentType":        dto.setEmploymentType(value); break;
            case "employeeCategory":      dto.setEmployeeCategory(value); break;
            case "employeeSeries":        dto.setEmployeeSeries(value); break;
            case "producerType":          dto.setProducerType(value); break;
            case "employeeReferenceNumber": dto.setEmployeeReferenceNumber(value); break;
            case "costCenter":            dto.setCostCenter(value); break;
            case "division":              dto.setDivision(value); break;
            case "grade":                 dto.setGrade(value); break;
            case "teamName":              dto.setTeamName(value); break;
            case "branchHead":            dto.setBranchHead(value); break;
            case "unitHead":              dto.setUnitHead(value); break;
            case "probationPeriodDays":   dto.setProbationPeriodDays(value); break;
            case "noticePeriodDays":      dto.setNoticePeriodDays(value); break;
            case "status":                dto.setStatus(value); break;

            // Device / login
            case "empCodeOnDevice":       dto.setEmpCodeOnDevice(value); break;
            case "loginUsername":         dto.setLoginUsername(value); break;

            // Emergency
            case "emergencyContactName":          dto.setEmergencyContactName(value); break;
            case "emergencyContactPhone":         dto.setEmergencyContactPhone(value); break;
            case "emergencyContactRelationship":  dto.setEmergencyContactRelationship(value); break;

            // Background
            case "backgroundCheckStatus":       dto.setBackgroundCheckStatus(value); break;
            case "backgroundVerificationDate":  dto.setBackgroundVerificationDate(value); break;
            case "backgroundAgencyName":        dto.setBackgroundAgencyName(value); break;
            case "backgroundCheckRemarks":      dto.setBackgroundCheckRemarks(value); break;

            // Banking
            case "bankAccountNumber":  dto.setBankAccountNumber(value); break;
            case "bankIfscCode":       dto.setBankIfscCode(value); break;
            case "bankName":           dto.setBankName(value); break;
            case "bankAccountType":    dto.setBankAccountType(value); break;
            case "bankBranch":         dto.setBankBranch(value); break;
            case "salaryPaymentMode":  dto.setSalaryPaymentMode(value); break;
            case "ddPayableAt":        dto.setDdPayableAt(value); break;
            case "nameAsPerBank":      dto.setNameAsPerBank(value); break;
            case "iban":               dto.setIban(value); break;

            // Statutory
            case "panNumber":          dto.setPanNumber(value); break;
            case "aadhaarNumber":      dto.setAadhaarNumber(value); break;
            case "aadhaarEnrolmentNo": dto.setAadhaarEnrolmentNo(value); break;
            case "aadhaarName":        dto.setAadhaarName(value); break;
            case "uanNumber":          dto.setUanNumber(value); break;
            case "pfEligible":         dto.setPfEligible(value); break;
            case "pfNumber":           dto.setPfNumber(value); break;
            case "pfScheme":           dto.setPfScheme(value); break;
            case "pfJoiningDate":      dto.setPfJoiningDate(value); break;
            case "excessEpfEligible":  dto.setExcessEpfEligible(value); break;
            case "excessEpsEligible":  dto.setExcessEpsEligible(value); break;
            case "existingPfMember":   dto.setExistingPfMember(value); break;
            case "esiEligible":        dto.setEsiEligible(value); break;
            case "esiNumber":          dto.setEsiNumber(value); break;
            case "lwfEligible":        dto.setLwfEligible(value); break;

            // Salary (CTC)
            case "structureType":      dto.setStructureType(value); break;
            case "monthlyGrossCtc":    dto.setMonthlyGrossCtc(value); break;
            case "nth":                dto.setNth(value); break;
            case "annualCtc":          dto.setAnnualCtc(value); break;
            case "tdsAmount":          dto.setTdsAmount(value); break;
            case "tdsRatePercent":     dto.setTdsRatePercent(value); break;
            case "employerPf":         dto.setEmployerPf(value); break;
            case "employeePf":         dto.setEmployeePf(value); break;
            case "employerEsi":        dto.setEmployerEsi(value); break;
            case "employeeEsi":        dto.setEmployeeEsi(value); break;
            case "lwfAmount":          dto.setLwfAmount(value); break;
            case "incentives":         dto.setIncentives(value); break;
            case "otherDeductions":    dto.setOtherDeductions(value); break;
            case "numOfMonths":        dto.setNumOfMonths(value); break;

            // Misc
            case "targetInfo":         dto.setTargetInfo(value); break;
            case "employeeRemarks":    dto.setEmployeeRemarks(value); break;
            case "offerLetterIssued":  dto.setOfferLetterIssued(value); break;
            case "idCardStatus":       dto.setIdCardStatus(value); break;
            case "punchingStatus":     dto.setPunchingStatus(value); break;

            default: /* unknown field — ignore */
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING: {
                    String val = cell.getStringCellValue().trim();
                    return val.isEmpty() ? null : val;
                }
                case NUMERIC: {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    double num = cell.getNumericCellValue();
                    if (num == Math.floor(num) && !Double.isInfinite(num)) {
                        return String.valueOf((long) num);
                    }
                    return String.valueOf(num);
                }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA: {
                    // A formula may cache a STRING, NUMERIC, BOOLEAN, or ERROR result.
                    // Blindly calling getNumericCellValue() on an ERROR-cached formula
                    // throws IllegalStateException ("Cannot get a NUMERIC value from a
                    // ERROR cell") and used to abort the entire 400-row import. Inspect
                    // the cached result type first so a single bad cell stays local.
                    CellType resultType = cell.getCachedFormulaResultType();
                    switch (resultType) {
                        case STRING: {
                            String val = cell.getStringCellValue();
                            return (val == null || val.trim().isEmpty()) ? null : val.trim();
                        }
                        case NUMERIC: {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                            }
                            double num = cell.getNumericCellValue();
                            if (num == Math.floor(num) && !Double.isInfinite(num)) {
                                return String.valueOf((long) num);
                            }
                            return String.valueOf(num);
                        }
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        case ERROR:
                        default:
                            // Formula evaluated to #REF! / #N/A / #DIV/0! etc. —
                            // treat as empty rather than failing the import.
                            return null;
                    }
                }
                case ERROR:
                    // Literal error cell (#REF!, #N/A, #DIV/0!, #VALUE!, #NAME?, #NULL!).
                    // Treat as empty so the import can continue.
                    return null;
                case BLANK:
                default:
                    return null;
            }
        } catch (Exception e) {
            // Defensive: never let a single malformed cell abort the whole import.
            // Log once at WARN with coordinates so HR can find and clean the cell.
            log.warn("Unreadable cell at sheet row {} col {} — treating as empty: {}",
                    cell.getRowIndex(), cell.getColumnIndex(), e.getMessage());
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidEmployee(EmployeeImportDTO dto) {
        boolean hasCode = dto.getEmployeeCode() != null && !dto.getEmployeeCode().trim().isEmpty();
        boolean hasName = (dto.getFirstName() != null && !dto.getFirstName().trim().isEmpty())
                || (dto.getLastName() != null && !dto.getLastName().trim().isEmpty())
                || (dto.getFullName() != null && !dto.getFullName().trim().isEmpty());

        if (!hasCode && !hasName) {
            log.debug("Row {} skipped: no employee code or name", dto.getRowNumber());
            return false;
        }
        if (hasCode && !hasName) {
            // Use the code as the first name so the DB NOT NULL constraint is satisfied
            dto.setFirstName(dto.getEmployeeCode());
            dto.setLastName("");
        }
        if (!hasCode && hasName) {
            dto.setEmployeeCode(generateTempCode(dto.getFirstName()));
        }
        return true;
    }

    private String generateTempCode(String name) {
        if (name == null || name.isEmpty()) {
            return "EMP-" + System.currentTimeMillis();
        }
        String code = name.toUpperCase().replaceAll("[^A-Z]", "");
        if (code.isEmpty()) code = "IMP";
        if (code.length() > 4) code = code.substring(0, 4);
        return "IMP-" + code + "-" + (System.currentTimeMillis() % 10000);
    }
}
