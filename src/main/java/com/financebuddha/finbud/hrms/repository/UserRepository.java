package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByEmployeeId(Long employeeId);

    boolean existsByUsername(String username);

    boolean existsByEmployeeId(Long employeeId);
}
