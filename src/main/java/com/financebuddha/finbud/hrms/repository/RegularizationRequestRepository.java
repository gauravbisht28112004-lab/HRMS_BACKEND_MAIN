package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.RegularizationRequest;
import com.financebuddha.finbud.hrms.enums.RegularizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegularizationRequestRepository extends JpaRepository<RegularizationRequest, Long> {

    List<RegularizationRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<RegularizationRequest> findByStatusOrderByCreatedAtDesc(RegularizationStatus status);

    /** Approval queue scoped to employees reporting to a given manager. */
    @Query("""
            SELECT r FROM RegularizationRequest r
            WHERE r.status = :status
              AND r.employee.manager.id = :managerId
            ORDER BY r.createdAt DESC
            """)
    List<RegularizationRequest> findByStatusAndManager(@Param("status") RegularizationStatus status,
                                                       @Param("managerId") Long managerId);

    /** Idempotency guard — prevent a second open request for the same day. */
    Optional<RegularizationRequest> findFirstByEmployeeIdAndAttendanceDateAndStatus(
            Long employeeId, LocalDate attendanceDate, RegularizationStatus status);
}
