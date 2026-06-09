package com.financebuddha.finbud.hrms.enums;

/**
 * State of an employee-filed attendance regularization request. Reuses the
 * same PENDING/APPROVED/REJECTED shape as attendance approval to keep the
 * UI consistent.
 */
public enum RegularizationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
