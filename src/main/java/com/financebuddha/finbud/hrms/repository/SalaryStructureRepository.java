package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    Optional<SalaryStructure> findByEmployeeId(Long employeeId);

    Optional<SalaryStructure> findByEmployeeIdAndIsActiveTrue(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);
}
