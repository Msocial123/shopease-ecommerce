package com.shopease.config;

import com.shopease.dao.UserDao;
import com.shopease.model.Role;
import com.shopease.model.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Creates the tables (idempotent CREATE TABLE IF NOT EXISTS), loads sample catalogue data
 * on an empty database, and makes sure an administrator account exists.
 * Disable with {@code app.db.init=false} once you manage the schema yourself.
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.db.init:true}")
    private boolean enabled;
    @Value("${app.admin.email:admin@shopease.com}")
    private String adminEmail;
    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbc, UserDao userDao, PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Database initialisation disabled (app.db.init=false)");
            return;
        }
        log.info("Running schema.sql ...");
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql"));
        schema.setSqlScriptEncoding("UTF-8");
        schema.execute(dataSource);

        Integer categories = jdbc.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        if (categories == null || categories == 0) {
            log.info("Empty catalogue - loading sample data from data.sql ...");
            ResourceDatabasePopulator data = new ResourceDatabasePopulator(new ClassPathResource("db/data.sql"));
            data.setSqlScriptEncoding("UTF-8");
            data.execute(dataSource);
        }

        if (userDao.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFullName("Store Administrator");
            admin.setEmail(adminEmail);
            admin.setPhone("9999999999");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            userDao.insert(admin);
            log.info("Created default admin account: {}", adminEmail);
        }
    }
}
