package com.group10.cinemabooking.configurations;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.sql.DataSource;

@Configuration
public class DatabaseConf {
    private final AppConf appConf;

    @Autowired
    public DatabaseConf(AppConf appConf) {
        this.appConf = appConf;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        //connection settings
        config.setJdbcUrl(appConf.getDatabase().getUrl());
        config.setUsername(appConf.getDatabase().getUsername());
        config.setPassword(appConf.getDatabase().getPassword());
        // --- Pool size ---
        config.setMaximumPoolSize(20);  // how many concurrent connections max
        config.setMinimumIdle(5);       // keep a couple idle ready

        // --- Timeouts ---
        // Close idle connections after 5 minutes
        config.setIdleTimeout(300000);      // 5 * 60 * 1000

        // Completely recycle connections after 20 minutes
        config.setMaxLifetime(1200000);     // 20 * 60 * 1000

        // Send a lightweight ping every 4 minutes to keep them alive
        // (must be < DB/firewall idle timeout; 4 min is a safe guess)
        config.setKeepaliveTime(240000);    // 4 * 60 * 1000

        // How long to wait when checking if a connection is valid
        config.setValidationTimeout(5000);  // 5 seconds

        return new HikariDataSource(config);
    }
}
