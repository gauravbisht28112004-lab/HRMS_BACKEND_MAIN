package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureRequest;
import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.SalaryStructureMapper;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.SalaryStructureRepository;
import com.financebuddha.finbud.hrms.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getSalaryStructure(Long employeeId) {
        log.info("Fetching salary structure for employee: {}", employeeId);

        SalaryStructure salaryStructure = salaryStructureRepository
                .findByEmployeeIdAndIsActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "employeeId", employeeId));

        return mapper.toResponse(salaryStructure);
    }

    @Override
    @Transactional
    public SalaryStructureResponse createSalaryStructure(Long employeeId, SalaryStructureRequest request) {
        log.info("Creating salary structure for employee: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (salaryStructureRepository.existsByEmployeeId(employeeId)) {
            throw new BadRequestException("Salary structure already exists for this employee");
        }

        SalaryStructure salaryStructure = mapper.toEntity(request);
        salaryStructure.setEmployee(employee);
        salaryStructure.setIsActive(true);

        salaryStructure = salaryStructureRepository.save(salaryStructure);
        log.info("Created salary structure for employee: {}", employeeId);

        return mapper.toResponse(salaryStructure);
    }

    @Override
    @Transactional
    public SalaryStructureResponse updateSalaryStructure(Long id, SalaryStructureRequest request) {
        log.info("Updating salary structure: {}", id);

        SalaryStructure existingStructure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id));

        // In-place update. We hold exactly one salary_structure per employee
        // (V9 migration enforces UNIQUE employee_id). Previously this method
        // deactivated the old row and inserted a new one — which violated
        // that invariant and caused Hibernate's @OneToOne(mappedBy="employee")
        // back-ref fetch on Employee to explode at boot time with:
        //     "More than one row with the given identifier was found: <id>,
        //      for class: SalaryStructure"
        // See Flyway V9__salary_structures_unique_employee.sql for the DB-side
        // fix and deduplication.
        //
        // MapStruct's @BeanMapping(IGNORE) nulls means optional fields left
        // blank on the request won't clobber server values — matches what
        // HR expects when editing just one line of the salary.
        mapper.updateEntityFromRequest(request, existingStructure);
        existingStructure.setIsActive(true);

        SalaryStructure updated = salaryStructureRepository.save(existingStructure);
        log.info("Updated salary structure: {}", id);

        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateSalaryStructure(Long id) {
        log.info("Deactivating salary structure: {}", id);

        SalaryStructure salaryStructure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id));

        salaryStructure.setIsActive(false);
        salaryStructureRepository.save(salaryStructure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> getAllSalaryStructures() {
        log.info("Fetching all salary structures");
        return mapper.toResponseList(salaryStructureRepository.findAll());
    }
}
