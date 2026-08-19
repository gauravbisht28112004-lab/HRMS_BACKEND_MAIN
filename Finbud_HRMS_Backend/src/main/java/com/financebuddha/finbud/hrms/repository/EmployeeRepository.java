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
     * Every employee id in the management subtree beneath {@code rootId},
     * at any depth — direct reports, their reports, and so on. The root
     * itself is NOT included.
     *
     * <p>Needed because the reporting chain is multi-level
     * (Manager → Team Leader → ATL → Employee) but daily commitments are
     * only ever filed by the leaf tier. A one-level {@code manager_id = ?}
     * lookup therefore returns nothing for anyone above a Team Leader.
     *
     * <p>Native query — Postgres {@code WITH RECURSIVE}, mirroring
     * {@code DailyCommitmentRepository#aggregateSubtreeDisbursalByBranch}
     * so subtree membership is defined identically in both places. Assumes
     * an acyclic hierarchy, which the management chain is by construction.
     */
    @Query(value = """
           WITH RECURSIVE subtree AS (
               SELECT e.id
                 FROM employees e
                WHERE e.manager_id = :rootId
               UNION ALL
               SELECT c.id
                 FROM employees c
                 JOIN subtree s ON c.manager_id = s.id
           )
           SELECT s.id FROM subtree s
           """, nativeQuery = true)
    List<Long> findSubtreeEmployeeIds(@Param("rootId") Long rootId);

    /**
     * True if {@code candidateId} sits anywhere in the management subtree
     * beneath {@code rootId}. Same recursion as
     * {@link #findSubtreeEmployeeIds(Long)} but stops at the first hit instead
     * of materialising the whole branch — used on the authorization path, which
     * runs on every report request.
     *
     * <p>Because the hierarchy is a single-parent tree, this returning true is
     * exactly the statement "this employee reports up to that supervisor,
     * directly or through a TL/ATL, and to no other supervisor".
     */
    @Query(value = """
           WITH RECURSIVE subtree AS (
               SELECT e.id
                 FROM employees e
                WHERE e.manager_id = :rootId
               UNION ALL
               SELECT c.id
                 FROM employees c
                 JOIN subtree s ON c.manager_id = s.id
           )
           SELECT EXISTS (SELECT 1 FROM subtree s WHERE s.id = :candidateId)
           """, nativeQuery = true)
    boolean existsInSubtree(@Param("rootId") Long rootId, @Param("candidateId") Long candidateId);

    /**
     * Employees by id with their supervisor eagerly attached. Used by the
     * commitment reports to build the summary roster without an N+1 on
     * {@code manager} when rendering the "Reports To" column.
     */
    @Query("""
           SELECT e FROM Employee e
             LEFT JOIN FETCH e.manager
            WHERE e.id IN :ids
            ORDER BY e.firstName ASC, e.lastName ASC
           """)
    List<Employee> findAllByIdInWithManager(@Param("ids") List<Long> ids);

    /**
     * All active employees that have no linked User row yet.
     * Used by the provision-missing endpoint to auto-create login accounts
     * for employees added directly to the DB without going through the import flow.
     */
    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' AND NOT EXISTS (SELECT u FROM User u WHERE u.employee = e)")
    List<Employee> findAllWithoutUserAccount();
}
