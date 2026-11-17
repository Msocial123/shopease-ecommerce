package com.shopease.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
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
public class RootConfig implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RootConfig.class);

    /** Pool we created ourselves (null when Tomcat provides the DataSource through JNDI). */
    private HikariDataSource ownPool;

    /**
     * Connection source, in order of preference:
     * <ol>
     *     <li>JNDI {@code java:comp/env/jdbc/ShopEaseDB} - a {@code <Resource>} defined in Tomcat's
     *         server.xml and linked by META-INF/context.xml (Tomcat owns the pool).</li>
     *     <li>Otherwise a HikariCP pool built from db.url / db.username / db.password
     *         (app.properties, -D system properties or DB_* environment variables).</li>
     * </ol>
     * destroyMethod is disabled so Spring never closes a container-managed pool.
     */
    @Bean(destroyMethod = "")
    public DataSource dataSource(Environment env) {
        String jndiName = env.getProperty("db.jndiName", "jdbc/ShopEaseDB");
        if (!jndiName.isBlank()) {
            try {
                JndiDataSourceLookup lookup = new JndiDataSourceLookup();
                lookup.setResourceRef(true); // prefixes java:comp/env/
                DataSource ds = lookup.getDataSource(jndiName);
                log.info("Using container DataSource from JNDI: java:comp/env/{}", jndiName);
                return ds;
            } catch (DataSourceLookupFailureException e) {
                log.info("No JNDI DataSource '{}' - using db.url settings with HikariCP", jndiName);
            }
        }
        return hikari(env);
    }

    /** Human-readable description of where connections come from (shown on /health). */
    @Bean
    public String dataSourceDescription(DataSource dataSource, Environment env) {
        return dataSource instanceof HikariDataSource
                ? "hikari:" + env.getProperty("db.url", "").replaceAll("\\?.*", "")
                : "jndi:java:comp/env/" + env.getProperty("db.jndiName", "jdbc/ShopEaseDB");
    }

    @Override
    public void destroy() {
        if (ownPool != null) ownPool.close();
    }

    private DataSource hikari(Environment env) {
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
        ownPool = new HikariDataSource(cfg);
        return ownPool;
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
