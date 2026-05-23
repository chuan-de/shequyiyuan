package com.hospital.auth.service;

public interface UserAccountService {
    /** Creates app_user + assigns the given roleCode; returns the new user_id. */
    long createUser(String username, String rawPassword, String roleCode);

    void setEnabled(long userId, boolean enabled);

    void resetPassword(long userId, String newPassword);
}
