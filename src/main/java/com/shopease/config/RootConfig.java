package com.shopease.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * Root application context: infrastructure + business layer.
 *
 * <p>Database settings come from {@code app.properties}; every key can be overridden
 * with a JVM system property (e.g. {@code -Ddb.password=secret} in CATALINA_OPTS) or an
 * environment variable (e.g. {@code DB_PASSWORD=secret}).</p>
 */
@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:app.properties", encoding = "UTF-8")
@ComponentScan(
        basePackages = "com.shopease",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {Controller.class, RestController.class, ControllerAdvice.class}),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {WebConfig.class})
        })
public class RootConfig {

    @Bean(destroyMethod = "close")
    public DataSource dataSource(Environment env) {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(env.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        cfg.setJdbcUrl(env.getRequiredProperty("db.url"));
        cfg.setUsername(env.getRequiredProperty("db.username"));
        cfg.setPassword(env.getProperty("db.password", ""));
        cfg.setMaximumPoolSize(env.getProperty("db.pool.max", Integer.class, 10));
        cfg.setMinimumIdle(env.getProperty("db.pool.minIdle", Integer.class, 2));
        cfg.setConnectionTimeout(env.getProperty("db.pool.connectionTimeoutMs", Long.class, 10_000L));
        cfg.setPoolName("ShopEasePool");
        // Start even when MySQL is unreachable: the app then reports the problem on /health and
        // on its error page instead of failing to deploy (which shows up as a bare Tomcat 404).
        cfg.setInitializationFailTimeout(-1);
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        return new HikariDataSource(cfg);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
