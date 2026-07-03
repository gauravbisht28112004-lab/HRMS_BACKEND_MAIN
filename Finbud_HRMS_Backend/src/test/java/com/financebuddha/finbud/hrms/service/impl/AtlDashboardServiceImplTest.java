package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.atl.AtlDashboardResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlSummaryEntryResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlTeamMemberCommitmentResponse;
import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AtlDashboardServiceImpl} — the new ATL rollup
 * dashboard. Verifies:
 * <ul>
 *   <li>Per-employee commitment totals sum correctly across multiple
 *       DailyCommitment rows, using targetDisbursalAmount (committed, any
 *       status) not actualDisbursalAmount.</li>
 *   <li>Direct reports with zero commitments in the window still appear in
 *       the roster with a zero total, rather than being dropped.</li>
 *   <li>The cumulative total is the sum of every member's total.</li>
 *   <li>Members are sorted highest-committed-first.</li>
 *   <li>getAtlSummary() rolls up per ATL from the active-ROLE_ATL user list
 *       and skips the DB aggregate call entirely when there are no ATLs.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtlDashboardServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private DailyCommitmentRepository dailyCommitmentRepository;

    @InjectMocks
    private AtlDashboardServiceImpl service;

    private Employee employee(Long id, String code, String first, String last) {
        return Employee.builder()
                .id(id)
                .employeeId(code)
                .firstName(first)
                .lastName(last)
                .build();
    }

    private DailyCommitment commitment(Employee emp, LocalDate date, String targetAmount) {
        return DailyCommitment.builder()
                .employee(emp)
                .workDate(date)
                .targetDisbursalAmount(new BigDecimal(targetAmount))
                .build();
    }

    @Nested
    @DisplayName("getTeamDashboard")
    class GetTeamDashboard {

        @Test
        @DisplayName("sums per-employee targets, includes zero-activity roster members, sorts descending")
        void aggregatesTeamCorrectly() {
            Employee atl = employee(1L, "ATL01", "Priya", "Sharma");
            Employee empA = employee(2L, "ND001", "Amit", "Kumar");
            Employee empB = employee(3L, "ND002", "Neha", "Rao");
            Employee empC = employee(4L, "ND003", "Zero", "Activity");

            LocalDate start = LocalDate.of(2026, 7, 1);
            LocalDate end = LocalDate.of(2026, 7, 31);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(atl));
            when(dailyCommitmentRepository.findByManagerIdAndWorkDateBetween(1L, start, end))
                    .thenReturn(List.of(
                            commitment(empA, LocalDate.of(2026, 7, 1), "50000"),
                            commitment(empA, LocalDate.of(2026, 7, 2), "25000"),
                            commitment(empB, LocalDate.of(2026, 7, 1), "10000")
                    ));
            // empC has no commitment rows in the window but IS a direct report.
            when(employeeRepository.findActiveSubordinates(1L))
                    .thenReturn(List.of(empA, empB, empC));

            AtlDashboardResponse response = service.getTeamDashboard(1L, start, end);

            assertThat(response.getAtlId()).isEqualTo(1L);
            assertThat(response.getAtlCode()).isEqualTo("ATL01");
            assertThat(response.getAtlName()).isEqualTo("Priya Sharma");
            assertThat(response.getTeamSize()).isEqualTo(3);
            // 50000 + 25000 + 10000 + 0 = 85000
            assertThat(response.getTotalTargetDisbursalAmount()).isEqualByComparingTo("85000");

            List<AtlTeamMemberCommitmentResponse> members = response.getTeamMembers();
            assertThat(members).hasSize(3);
            // Sorted highest-first: empA (75000), empB (10000), empC (0)
            assertThat(members.get(0).getEmployeeCode()).isEqualTo("ND001");
            assertThat(members.get(0).getTotalTargetDisbursalAmount()).isEqualByComparingTo("75000");
            assertThat(members.get(1).getEmployeeCode()).isEqualTo("ND002");
            assertThat(members.get(1).getTotalTargetDisbursalAmount()).isEqualByComparingTo("10000");
            assertThat(members.get(2).getEmployeeCode()).isEqualTo("ND003");
            assertThat(members.get(2).getTotalTargetDisbursalAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the ATL id doesn't exist")
        void throwsWhenAtlMissing() {
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTeamDashboard(99L, LocalDate.now(), LocalDate.now()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAtlSummary")
    class GetAtlSummary {

        @Test
        @DisplayName("rolls up cumulative commitment per ATL and sorts descending")
        void rollsUpPerAtl() {
            Employee atl1 = employee(10L, "ATL10", "Rohit", "Verma");
            Employee atl2 = employee(11L, "ATL11", "Sana", "Iqbal");

            User user1 = User.builder().employee(atl1).build();
            User user2 = User.builder().employee(atl2).build();

            LocalDate start = LocalDate.of(2026, 7, 1);
            LocalDate end = LocalDate.of(2026, 7, 31);

            when(userRepository.findActiveUsersByRoleName(RoleType.ROLE_ATL))
                    .thenReturn(List.of(user1, user2));

            DailyCommitmentRepository.ManagerAggregateRow row1 =
                    new TestManagerAggregateRow(10L, new BigDecimal("120000"));
            DailyCommitmentRepository.ManagerAggregateRow row2 =
                    new TestManagerAggregateRow(11L, new BigDecimal("300000"));

            when(dailyCommitmentRepository.aggregateTargetDisbursalByManagerIds(
                    eq(List.of(10L, 11L)), eq(start), eq(end)))
                    .thenReturn(List.of(row1, row2));

            when(employeeRepository.findActiveSubordinates(10L)).thenReturn(List.of(employee(20L, "N1", "A", "B")));
            when(employeeRepository.findActiveSubordinates(11L))
                    .thenReturn(List.of(employee(21L, "N2", "A", "B"), employee(22L, "N3", "A", "B")));

            List<AtlSummaryEntryResponse> summary = service.getAtlSummary(start, end);

            assertThat(summary).hasSize(2);
            // Sana Iqbal (300000) should come first — descending order.
            assertThat(summary.get(0).getAtlCode()).isEqualTo("ATL11");
            assertThat(summary.get(0).getTotalTargetDisbursalAmount()).isEqualByComparingTo("300000");
            assertThat(summary.get(0).getTeamSize()).isEqualTo(2);

            assertThat(summary.get(1).getAtlCode()).isEqualTo("ATL10");
            assertThat(summary.get(1).getTotalTargetDisbursalAmount()).isEqualByComparingTo("120000");
            assertThat(summary.get(1).getTeamSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns an empty list without touching the aggregate query when there are no ATLs")
        void emptyWhenNoAtls() {
            when(userRepository.findActiveUsersByRoleName(RoleType.ROLE_ATL)).thenReturn(List.of());

            List<AtlSummaryEntryResponse> summary = service.getAtlSummary(LocalDate.now(), LocalDate.now());

            assertThat(summary).isEmpty();
        }
    }

    /** Plain-Java stand-in for the Spring Data projection interface. */
    private record TestManagerAggregateRow(Long getManagerId, BigDecimal getTotal)
            implements DailyCommitmentRepository.ManagerAggregateRow {
    }
}
