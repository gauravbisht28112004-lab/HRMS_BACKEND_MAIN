package com.financebuddha.finbud.hrms.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Diagnostic-only test (not part of the committed suite): boots a real JPA
 * EntityManagerFactory against an in-memory H2 database and actually
 * executes the new ATL @Query methods, so Hibernate parses + validates the
 * JPQL the same way it would on a real server boot. Mockito-based unit
 * tests never exercise this path, so this is the only local way to catch
 * a JPQL/path-expression error before it reaches production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AtlJpqlSmokeTest {

    @Autowired
    private DailyCommitmentRepository dailyCommitmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void aggregateTargetDisbursalByManagerIds_executesWithoutJpqlError() {
        assertThatCode(() ->
                dailyCommitmentRepository.aggregateTargetDisbursalByManagerIds(
                        List.of(1L, 2L), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        ).doesNotThrowAnyException();
    }

    @Test
    void findActiveSubordinates_executesWithoutJpqlError() {
        assertThatCode(() -> employeeRepository.findActiveSubordinates(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void findActiveUsersByRoleName_executesWithoutJpqlError() {
        assertThatCode(() ->
                userRepository.findActiveUsersByRoleName(
                        com.financebuddha.finbud.hrms.enums.RoleType.ROLE_ATL))
                .doesNotThrowAnyException();
    }
}
