package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    // Used by the Excel import to match department names case-insensitively
    // (e.g. "sales" / "Sales " / "SALES" all resolve to the same Department).
    @Query("SELECT d FROM Department d WHERE LOWER(TRIM(d.name)) = LOWER(TRIM(:name))")
    Optional<Department> findByNameIgnoreCase(@Param("name") String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    Page<Department> findByManagerId(Long managerId, Pageable pageable);

    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Department> searchByName(@Param("search") String search, Pageable pageable);

    // Active-only headcount for a department. Drives the dashboard "Department
    // Distribution" and the department page headcount, so inactive employees are
    // not shown.
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId AND e.status = com.financebuddha.finbud.hrms.enums.EmployeeStatus.ACTIVE")
    Long countEmployeesByDepartment(@Param("departmentId") Long departmentId);

    // Unfiltered count (all statuses). Used by the delete guard so a department
    // that still has inactive employees attached cannot be deleted.
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId")
    Long countAllEmployeesByDepartment(@Param("departmentId") Long departmentId);
}
