package com.group10.cinemabooking.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConf {
    private final AppConf appConf;

    @Autowired
    public PayOSConf(AppConf appConf) {
        this.appConf = appConf;
    }

//    @Bean
//    public PayOS payOS() {
//        return new PayOS(
//                System.getenv(appConf.getPaySecret().getClientId()),
//                System.getenv(appConf.getPaySecret().getApiKey()),
//                System.getenv(appConf.getPaySecret().getChecksumKey())
//        );
//    }
}
