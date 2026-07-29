package com.financebuddha.finbud.hrms.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.financebuddha.finbud.hrms.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;

    /** Business-facing employee code (e.g. "ND33004"). Used in URLs and UI. */
    private String employeeId;

    /**
     * Primary-key id of the linked Employee row, distinct from {@link #employeeId}.
     * This is the value used by {@code @PathVariable Long employeeId} on most
     * controller endpoints, so security expressions like
     * {@code @authz.isOwner(#employeeId)} compare against this field.
     */
    private Long employeePrimaryId;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmployee() != null ? user.getEmployee().getEmail() : null)
                .employeeId(user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null)
                .employeePrimaryId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
