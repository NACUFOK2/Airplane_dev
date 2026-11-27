package com.airplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AirplaneApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirplaneApplication.class, args);
    }
}
