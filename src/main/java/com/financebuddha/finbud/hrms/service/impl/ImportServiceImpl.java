package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.employee.EmployeeCreateResponse;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeRequest;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse.ImportResult;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.EmployeeService;
import com.financebuddha.finbud.hrms.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;

    @Override
    public ImportResponse importEmployees(MultipartFile file) throws Exception {
        log.info("Importing employees from file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String fileName = file.getOriginalFilename().toLowerCase();
        boolean isXlsx = fileName.endsWith(".xlsx");
        boolean isXls = fileName.endsWith(".xls");
        boolean isCsv = fileName.endsWith(".csv");

        if (!isXlsx && !isXls && !isCsv) {
            throw new BadRequestException("File must be .xls, .xlsx, or .csv");
        }

        List<ImportResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = isXlsx || isXls ? WorkbookFactory.create(inputStream) : null;

            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0);
                results = processExcelSheet(sheet);
                workbook.close();
            } else {
                results = processCSV(inputStream);
            }
        }

        for (ImportResult result : results) {
            if ("SUCCESS".equals(result.getStatus())) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        return ImportResponse.builder()
                .totalRecords(results.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    private List<ImportResult> processExcelSheet(Sheet sheet) {
        List<ImportResult> results = new ArrayList<>();
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();

        Row headerRow = sheet.getRow(firstRowNum);
        if (headerRow == null) {
            return results;
        }

        for (int i = firstRowNum + 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            ImportResult result = processRow(row, i + 1);
            results.add(result);
        }

        return results;
    }

    private List<ImportResult> processCSV(InputStream inputStream) throws IOException {
        List<ImportResult> results = new ArrayList<>();
        List<String> lines = Arrays.asList(new String(inputStream.readAllBytes()).split("\n"));

        if (lines.size() <= 1) {
            return results;
        }

        String[] headers = lines.get(0).trim().split(",");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] values = line.split(",");
            ImportResult result = processCSVRow(values, headers, i + 1);
            results.add(result);
        }

        return results;
    }

    private ImportResult processRow(Row row, int rowNum) {
        try {
            EmployeeRequest request = new EmployeeRequest();

            request.setFirstName(getCellValue(row.getCell(0)));
            request.setLastName(getCellValue(row.getCell(1)));
            request.setEmail(getCellValue(row.getCell(2)));
            request.setPhone(getCellValue(row.getCell(3)));
            request.setDateOfJoining(getDateCellValue(row.getCell(4)));
            request.setDesignation(getCellValue(row.getCell(5)));
            request.setDepartmentId(getLongCellValue(row.getCell(6)));

            EmployeeCreateResponse created = employeeService.createEmployee(request);

            return ImportResult.builder()
                    .rowNumber(rowNum)
                    .employeeCode(created.getEmployee().getEmployeeId())
                    .status("SUCCESS")
                    .message("Employee imported successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error importing row {}: {}", rowNum, e.getMessage());
            return ImportResult.builder()
                    .rowNumber(rowNum)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }

    private ImportResult processCSVRow(String[] values, String[] headers, int rowNum) {
        try {
            EmployeeRequest request = new EmployeeRequest();

            for (int i = 0; i < headers.length && i < values.length; i++) {
                String header = headers[i].trim().toLowerCase();
                String value = values[i].trim();

                switch (header) {
                    case "firstname", "first_name", "first name":
                        request.setFirstName(value);
                        break;
                    case "lastname", "last_name", "last name":
                        request.setLastName(value);
                        break;
                    case "email":
                        request.setEmail(value);
                        break;
                    case "phone":
                        request.setPhone(value);
                        break;
                    case "dateofjoining", "date_of_joining", "doj", "joining date":
                        request.setDateOfJoining(LocalDate.parse(value));
                        break;
                    case "designation":
                        request.setDesignation(value);
                        break;
                    case "departmentid", "department_id", "department":
                        request.setDepartmentId(Long.valueOf(value));
                        break;
                }
            }

            EmployeeCreateResponse created = employeeService.createEmployee(request);

            return ImportResult.builder()
                    .rowNumber(rowNum)
                    .employeeCode(created.getEmployee().getEmployeeId())
                    .status("SUCCESS")
                    .message("Employee imported successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error importing CSV row {}: {}", rowNum, e.getMessage());
            return ImportResult.builder()
                    .rowNumber(rowNum)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = getCellValue(cell);
        if (value != null && !value.isEmpty()) {
            return LocalDate.parse(value);
        }
        return null;
    }

    private Long getLongCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        String value = getCellValue(cell);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}