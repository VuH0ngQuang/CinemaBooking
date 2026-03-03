package com.group10.cinemabooking.configurations;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DotEnvPostProcessor implements EnvironmentPostProcessor {
    public static final Logger log = LoggerFactory.getLogger(DotEnvPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> envMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                // Add to environment as a property source
                // This makes them available for ${VAR_NAME} resolution in application.yaml
                envMap.put(entry.getKey(), entry.getValue());
            });

            if (!envMap.isEmpty()) {
                MapPropertySource dotEnvSource = new MapPropertySource("dotenv", envMap);
                environment.getPropertySources().addFirst(dotEnvSource);
            }
        } catch (Exception e) {
            log.error("Warning: Could not load .env file: {}", e.getMessage());
        }
    }
}