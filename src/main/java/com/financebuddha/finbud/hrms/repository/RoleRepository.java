package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}
