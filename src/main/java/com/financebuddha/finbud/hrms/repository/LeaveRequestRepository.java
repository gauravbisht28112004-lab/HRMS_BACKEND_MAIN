package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByManagerId(Long managerId, Pageable pageable);

    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND " +
           "lr.status = 'APPROVED' AND " +
           "((lr.startDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.endDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.startDate <= :startDate AND lr.endDate >= :endDate))")
    List<LeaveRequest> findApprovedLeavesByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                                 @Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.manager.id = :managerId AND lr.status = 'PENDING'")
    List<LeaveRequest> findPendingLeavesForManager(@Param("managerId") Long managerId);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND lr.status = 'APPROVED' AND YEAR(lr.startDate) = :year")
    Long countApprovedLeavesByEmployeeAndYear(@Param("employeeId") Long employeeId, @Param("year") int year);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING' AND lr.startDate <= :date")
    List<LeaveRequest> findPendingLeavesPastStartDate(@Param("date") LocalDate date);
}
