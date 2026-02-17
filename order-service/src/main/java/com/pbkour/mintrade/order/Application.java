package com.pbkour.mintrade.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.pbkour.mintrade.order.repositories")
@EntityScan("com.pbkour.mintrade.contracts.db")
@ComponentScan(basePackages = {
    "com.pbkour.mintrade.order",
    "com.pbkour.mintrade.contracts.kafka",
    "com.pbkour.mintrade.contracts.json"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


