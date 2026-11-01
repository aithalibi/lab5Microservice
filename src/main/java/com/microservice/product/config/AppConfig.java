package com.microservice.product.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.microservice.product")
@Import({HibernateConfig.class, TransactionConfig.class})
public class AppConfig {
    // Cette classe centralise le balayage des composants Spring
}
