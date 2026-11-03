package com.humolix.volur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class VolurLiteApplication {

    static void main(String[] args) {
        SpringApplication.run(VolurLiteApplication.class, args);
    }

}
