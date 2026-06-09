package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureRequest;
import com.financebuddha.finbud.hrms.dto.salary.SalaryStructureResponse;

import java.util.List;

public interface SalaryService {

    SalaryStructureResponse getSalaryStructure(Long employeeId);

    SalaryStructureResponse createSalaryStructure(Long employeeId, SalaryStructureRequest request);

    SalaryStructureResponse updateSalaryStructure(Long id, SalaryStructureRequest request);

    void deactivateSalaryStructure(Long id);

    List<SalaryStructureResponse> getAllSalaryStructures();
}
