package com.winup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
@EntityScan("com.winup.entity")
@EnableJpaRepositories("com.winup.repository")
public class WinUpApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinUpApplication.class, args);
    }
}
