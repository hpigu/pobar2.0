package com.pobar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PobarApplication {

    public static void main(String[] args) {
        SpringApplication.run(PobarApplication.class, args);
    }

}
