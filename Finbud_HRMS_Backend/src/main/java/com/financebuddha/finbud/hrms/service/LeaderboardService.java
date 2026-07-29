package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.commitment.LeaderboardEntryResponse;

import java.util.List;

public interface LeaderboardService {

    /**
     * Ranked list of employees by total APPROVED disbursal in the given
     * (year, month). Q3 of the feature triage. Any authenticated user can
     * read — there's nothing sensitive in a public leaderboard.
     */
    List<LeaderboardEntryResponse> monthlyDisbursal(Integer year, Integer month);
}
