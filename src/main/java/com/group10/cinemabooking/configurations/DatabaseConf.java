package com.group10.cinemabooking.configurations;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.sql.DataSource;

@Configuration
public class DatabaseConf {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConf.class);
    private final AppConf appConf;

    @Autowired
    public DatabaseConf(AppConf appConf) {
        this.appConf = appConf;
    }

    @Bean
    public DataSource dataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            //connection settings
            config.setJdbcUrl(appConf.getDatabase().getUrl()+ "?rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true");
            config.setUsername(appConf.getDatabase().getUsername());
            config.setPassword(appConf.getDatabase().getPassword());
            // --- Pool size ---
            config.setMaximumPoolSize(40);  // how many concurrent connections max
            config.setMinimumIdle(10);       // keep a couple idle ready

            // --- Timeouts ---
            // Close idle connections after 5 minutes
            config.setIdleTimeout(300000);      // 5 * 60 * 1000
            config.setConnectionTimeout(3000);

            // Completely recycle connections after 20 minutes
            config.setMaxLifetime(1200000);     // 20 * 60 * 1000

            // Send a lightweight ping every 4 minutes to keep them alive
            // (must be < DB/firewall idle timeout; 4 min is a safe guess)
            config.setKeepaliveTime(240000);    // 4 * 60 * 100018gb

            // How long to wait when checking if a connection is valid
            config.setValidationTimeout(5000);  // 5 seconds

            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Failed to configure DataSource", e);
            throw new IllegalStateException("Cannot initialize DataSource", e);
        }
    }
}
