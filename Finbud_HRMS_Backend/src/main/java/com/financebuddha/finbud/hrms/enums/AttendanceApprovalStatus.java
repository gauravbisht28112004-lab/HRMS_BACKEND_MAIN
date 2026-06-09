package com.financebuddha.finbud.hrms.enums;

/**
 * Workflow state for portal-marked attendance.
 *
 * <p>Every new punch from the portal starts as {@link #PENDING} and needs
 * to be approved by either the employee's team-leader, HR, or an Admin
 * before it counts. Rejections require a reason.</p>
 */
public enum AttendanceApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
