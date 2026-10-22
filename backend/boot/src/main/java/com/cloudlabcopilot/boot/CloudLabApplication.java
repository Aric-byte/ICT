package com.cloudlabcopilot.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cloudlabcopilot")
public class CloudLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudLabApplication.class, args);
    }
}