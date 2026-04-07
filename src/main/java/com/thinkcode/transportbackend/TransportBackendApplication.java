package com.thinkcode.transportbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.thinkcode.transportbackend.entity")
@EnableJpaRepositories("com.thinkcode.transportbackend.repository")
public class TransportBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportBackendApplication.class, args);
    }
}
