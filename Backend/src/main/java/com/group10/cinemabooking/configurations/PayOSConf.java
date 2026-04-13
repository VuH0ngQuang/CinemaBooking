package com.group10.cinemabooking.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;

@Configuration
public class PayOSConf {
    private static final Logger log = LoggerFactory.getLogger(PayOSConf.class);
    private final AppConf appConf;

    @Autowired
    public PayOSConf(AppConf appConf) {
        this.appConf = appConf;
    }

    @Bean
    public PayOS payOS() {
        return new PayOS(
                appConf.getPaySecret().getClientId(),
                appConf.getPaySecret().getApiKey(),
                appConf.getPaySecret().getChecksumKey()
        );
    }
}
