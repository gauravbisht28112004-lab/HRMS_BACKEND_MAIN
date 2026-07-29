package com.financebuddha.finbud.hrms.enums;

/**
 * State machine for a daily commitment row:
 *
 * <pre>
 *      DRAFT  -- employee fills targets/actuals, can edit freely
 *        ↓
 *   SUBMITTED -- employee clicks Submit, locked to TL approval
 *        ↓
 *   APPROVED  -- TL/HR/Admin approved, counts towards leaderboard
 *      OR
 *   REJECTED  -- TL rejected with reason, employee can revise & re-submit
 * </pre>
 *
 * <p>Q3 leaderboard SUMs only over {@link #APPROVED} rows so unverified
 * data doesn't pollute rankings.
 */
public enum CommitmentStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}
