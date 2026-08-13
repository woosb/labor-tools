package dev.sungbin.labortools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LaborToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaborToolsApplication.class, args);
    }
}
