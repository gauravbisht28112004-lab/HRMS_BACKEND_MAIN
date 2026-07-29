package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByEmployeeId(Long employeeId);

    boolean existsByUsername(String username);

    boolean existsByEmployeeId(Long employeeId);

    /**
     * All active users linked to an employee who hold the given role.
     * Used by the notification fan-out to resolve recipients (HR/Admin
     * users who should be informed about leave state changes).
     */
    @Query("""
           SELECT u FROM User u
             JOIN u.roles r
            WHERE r.name = :roleName
              AND u.isActive = true
              AND u.employee IS NOT NULL
           """)
    List<User> findActiveUsersByRoleName(@Param("roleName") RoleType roleName);

    /**
     * All users who have never logged in (i.e. their initial provisioning
     * password is still in place). Used by the bulk-reset admin endpoint.
     */
    List<User> findByPasswordChangedAtIsNull();
}
