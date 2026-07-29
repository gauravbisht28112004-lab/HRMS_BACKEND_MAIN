package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.commitment.LeaderboardEntryResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * Q3 — leaderboard endpoint. Aggregate over APPROVED daily commitments;
 * no new tables. Any authenticated user can read.
 */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Sales-disbursal leaderboard")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/disbursal")
    @Operation(summary = "Monthly disbursal leaderboard",
               description = "Year + month default to the current period if omitted.")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> monthlyDisbursal(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        YearMonth target = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
        return ResponseEntity.ok(ApiResponse.success(
                leaderboardService.monthlyDisbursal(target.getYear(), target.getMonthValue())));
    }
}
