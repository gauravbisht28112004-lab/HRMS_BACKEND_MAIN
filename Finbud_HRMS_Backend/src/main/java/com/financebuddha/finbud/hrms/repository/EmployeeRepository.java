package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmpCodeOnDevice(Integer empCodeOnDevice);

    Optional<Employee> findByLoginUsername(String loginUsername);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByEmpCodeOnDevice(Integer empCodeOnDevice);

    boolean existsByLoginUsername(String loginUsername);

    List<Employee> findAllByEmployeeIdIn(List<String> employeeIds);

    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    Page<Employee> findByStatusNot(EmployeeStatus status, Pageable pageable);

    /** Non-paginated variant used by bulk background jobs (e.g. yearly leave allocation). */
    List<Employee> findAllByStatus(EmployeeStatus status);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Employee> findByManagerId(Long managerId, Pageable pageable);

    Page<Employee> findByManagerIdAndStatus(Long managerId, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByShiftTypeId(Long shiftTypeId, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Employee> searchEmployees(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:managerId IS NULL OR e.manager.id = :managerId)")
    Page<Employee> findByFilters(@Param("departmentId") Long departmentId,
                                   @Param("status") EmployeeStatus status,
                                   @Param("managerId") Long managerId,
                                   Pageable pageable);

    List<Employee> findByStatusAndDateOfJoiningBefore(EmployeeStatus status, LocalDate date);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = :status")
    Long countByStatus(@Param("status") EmployeeStatus status);

    @Query("SELECT e FROM Employee e WHERE e.manager.id = :managerId AND e.status = 'ACTIVE'")
    List<Employee> findActiveSubordinates(@Param("managerId") Long managerId);

    /**
     * All active employees that have no linked User row yet.
     * Used by the provision-missing endpoint to auto-create login accounts
     * for employees added directly to the DB without going through the import flow.
     */
    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' AND NOT EXISTS (SELECT u FROM User u WHERE u.employee = e)")
    List<Employee> findAllWithoutUserAccount();
}
