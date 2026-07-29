package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.OfficeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeLocationRepository extends JpaRepository<OfficeLocation, Long> {

    Optional<OfficeLocation> findByName(String name);

    List<OfficeLocation> findByIsActiveTrueOrderByNameAsc();
}
