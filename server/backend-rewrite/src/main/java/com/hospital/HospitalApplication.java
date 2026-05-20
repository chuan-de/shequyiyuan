package com.hospital;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HospitalApplication {

    private static final Logger log = LoggerFactory.getLogger(HospitalApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(HospitalApplication.class, args);
    }

    @Bean
    ApplicationRunner moduleMarkerLogger(
        @Value("${spring.application.name:hospital-backend-rewrite}") String applicationName
    ) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                log.info("[MODULE-MARKER] applicationName={}, modulePath=server/backend-rewrite", applicationName);
            }
        };
    }
}
