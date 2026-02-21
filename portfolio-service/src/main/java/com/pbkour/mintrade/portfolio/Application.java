package com.pbkour.mintrade.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.pbkour.mintrade.portfolio.repositories")
@EntityScan("com.pbkour.mintrade.portfolio.entities")
@ComponentScan(basePackages = {
    "com.pbkour.mintrade.portfolio",
    "com.pbkour.mintrade.commons"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


