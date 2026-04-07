package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftTypeRepository extends JpaRepository<ShiftType, Long> {

    Optional<ShiftType> findByCode(String code);

    boolean existsByCode(String code);
}
