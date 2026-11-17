package com.shopease.dao;

import com.shopease.model.Role;
import com.shopease.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserDao {

    private static final RowMapper<User> MAPPER = (rs, i) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return u;
    };

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findById(long id) {
        return jdbc.query("SELECT * FROM users WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE LOWER(email) = LOWER(?)", MAPPER, email).stream().findFirst();
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users ORDER BY created_at DESC", MAPPER);
    }

    public long insert(User u) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (full_name, email, password_hash, phone, role, active) VALUES (?,?,?,?,?,?)",
                    new String[]{"id"});
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getRole().name());
            ps.setBoolean(6, u.isActive());
            return ps;
        }, kh);
        long id = Objects.requireNonNull(kh.getKey()).longValue();
        u.setId(id);
        return id;
    }

    public void updateProfile(long id, String fullName, String phone) {
        jdbc.update("UPDATE users SET full_name = ?, phone = ? WHERE id = ?", fullName, phone, id);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE users SET active = ? WHERE id = ?", active, id);
    }

    public void setRole(long id, Role role) {
        jdbc.update("UPDATE users SET role = ? WHERE id = ?", role.name(), id);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'", Long.class);
        return n == null ? 0 : n;
    }
}
