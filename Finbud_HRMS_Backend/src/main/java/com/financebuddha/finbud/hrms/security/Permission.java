package com.financebuddha.finbud.hrms.security;

public final class Permission {

    private Permission() {}

    // Employee permissions
    public static final String EMPLOYEE_READ = "EMPLOYEE_READ";
    public static final String EMPLOYEE_WRITE = "EMPLOYEE_WRITE";
    public static final String EMPLOYEE_DELETE = "EMPLOYEE_DELETE";

    // Attendance permissions
    public static final String ATTENDANCE_READ = "ATTENDANCE_READ";
    public static final String ATTENDANCE_WRITE = "ATTENDANCE_WRITE";
    public static final String ATTENDANCE_MODIFY = "ATTENDANCE_MODIFY";

    // Leave permissions
    public static final String LEAVE_READ = "LEAVE_READ";
    public static final String LEAVE_APPLY = "LEAVE_APPLY";
    public static final String LEAVE_APPROVE = "LEAVE_APPROVE";

    // Payroll permissions
    public static final String PAYROLL_READ = "PAYROLL_READ";
    public static final String PAYROLL_GENERATE = "PAYROLL_GENERATE";
    public static final String PAYROLL_APPROVE = "PAYROLL_APPROVE";

    // Department permissions
    public static final String DEPARTMENT_READ = "DEPARTMENT_READ";
    public static final String DEPARTMENT_WRITE = "DEPARTMENT_WRITE";

    // Report permissions
    public static final String REPORT_READ = "REPORT_READ";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";

    // Admin permissions
    public static final String ADMIN_FULL = "ADMIN_FULL";
    public static final String CONFIG_WRITE = "CONFIG_WRITE";
}
