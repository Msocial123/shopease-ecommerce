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
    private final DbDialect dialect;

    @Value("${app.db.init:true}")
    private boolean enabled;
    @Value("${app.admin.email:admin@shopease.com}")
    private String adminEmail;
    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbc, UserDao userDao, PasswordEncoder passwordEncoder,
                               DbDialect dialect) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.dialect = dialect;
    }

    @Value("#{dataSourceDescription}")
    private String dataSourceDescription;
    @Value("${db.username:}")
    private String dbUser;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Database initialisation disabled (app.db.init=false)");
            return;
        }
        try {
            initialise();
        } catch (Exception e) {
            // Keep the web app deployed so /health and the error pages can explain the problem.
            log.error("""

                    ************************************************************************
                     ShopEase could not reach / initialise the database.
                       Source: {}
                       User  : {}
                       Cause : {}
                     Check that the database (MySQL or PostgreSQL) is running and that the connection settings are correct:
                     the jdbc/ShopEaseDB <Resource> in Tomcat's conf/server.xml, or else
                     db.url / db.username / db.password (DB_* in bin/setenv.sh).
                     Then restart Tomcat. Status: GET /shopease/health
                    ************************************************************************""",
                    dataSourceDescription, dataSourceDescription.startsWith("jndi:") ? "(from server.xml)" : dbUser,
                    rootCause(e).toString());
        }
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }

    private void initialise() {
        boolean postgres = dialect.isPostgres();
        String schemaFile = postgres ? "db/schema-postgresql.sql" : "db/schema.sql";
        log.info("Running {} ...", schemaFile);
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource(schemaFile));
        schema.setSqlScriptEncoding("UTF-8");
        schema.execute(dataSource);

        Integer categories = jdbc.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        if (categories == null || categories == 0) {
            log.info("Empty catalogue - loading sample data from data.sql ...");
            ResourceDatabasePopulator data = new ResourceDatabasePopulator(new ClassPathResource("db/data.sql"));
            data.setSqlScriptEncoding("UTF-8");
            data.execute(dataSource);
            if (postgres) {
                // data.sql inserts categories with explicit ids; move the identity sequence past them
                jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('categories', 'id'), " +
                        "(SELECT MAX(id) FROM categories))", Long.class);
            }
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
