package com.catalyst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class) // no default in-memory user; auth is ours
@ConfigurationPropertiesScan
@EnableAsync
public class CatalystApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalystApplication.class, args);
    }
}
