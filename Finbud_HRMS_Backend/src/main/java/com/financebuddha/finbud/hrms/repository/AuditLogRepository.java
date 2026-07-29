package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTableName(String tableName, Pageable pageable);

    Page<AuditLog> findByPerformedById(Long performedById, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.tableName = :tableName AND al.recordId = :recordId")
    List<AuditLog> findByTableNameAndRecordId(@Param("tableName") String tableName, @Param("recordId") Long recordId);

    @Query("SELECT al FROM AuditLog al WHERE al.performedAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.tableName = :tableName AND al.performedAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByTableNameAndDateRange(@Param("tableName") String tableName,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
}
