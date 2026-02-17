package org.app.autfmi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AutfmiendpointApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutfmiendpointApplication.class, args);
    }
}
