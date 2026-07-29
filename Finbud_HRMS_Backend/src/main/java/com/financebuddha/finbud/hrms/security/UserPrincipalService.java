package com.financebuddha.finbud.hrms.security;

import org.springframework.stereotype.Service;

@Service("userPrincipalService")
public class UserPrincipalService {

    public boolean isEmployee(Long employeeId) {
        UserPrincipal currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getEmployeeId() == null) {
            return false;
        }
        // Convert employeeId (Long) to String for comparison
        return currentUser.getEmployeeId().equals(employeeId.toString());
    }

    private UserPrincipal getCurrentUser() {
        org.springframework.security.core.context.SecurityContext context =
            org.springframework.security.core.context.SecurityContextHolder.getContext();
        if (context.getAuthentication() != null &&
            context.getAuthentication().getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) context.getAuthentication().getPrincipal();
        }
        return null;
    }
}
