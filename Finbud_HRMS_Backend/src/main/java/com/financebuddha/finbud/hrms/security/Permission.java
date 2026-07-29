package com.financebuddha.finbud.hrms.security;

public final class Permission {

    private Permission() {}

    // Employee permissions
    public static final String EMPLOYEE_READ = "EMPLOYEE_READ";
    public static final String EMPLOYEE_WRITE = "EMPLOYEE_WRITE";
    public static final String EMPLOYEE_DELETE = "EMPLOYEE_DELETE";

    // Department permissions
    public static final String DEPARTMENT_READ = "DEPARTMENT_READ";
    public static final String DEPARTMENT_WRITE = "DEPARTMENT_WRITE";

    // Admin permissions
    public static final String ADMIN_FULL = "ADMIN_FULL";
    public static final String CONFIG_WRITE = "CONFIG_WRITE";
}
