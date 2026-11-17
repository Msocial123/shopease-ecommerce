package com.shopease.model;

import java.io.Serial;
import java.io.Serializable;

/** Minimal, serialisable view of the logged-in user kept in the HTTP session. */
public class SessionUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "currentUser";

    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;

    public SessionUser(Long id, String fullName, String email, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public static SessionUser of(User u) {
        return new SessionUser(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) return "";
        return fullName.trim().split("\\s+")[0];
    }
}
