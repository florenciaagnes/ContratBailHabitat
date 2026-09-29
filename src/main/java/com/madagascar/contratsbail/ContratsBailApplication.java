package com.madagascar.contratsbail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContratsBailApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContratsBailApplication.class, args);
    }
}
