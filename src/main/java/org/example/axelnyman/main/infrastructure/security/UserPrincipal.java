package org.example.axelnyman.main.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long householdId;
    private final String email;
    private final String password;

    public UserPrincipal(Long userId, Long householdId, String email, String password) {
        this.userId = userId;
        this.householdId = householdId;
        this.email = email;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getHouseholdId() {
        return householdId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
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