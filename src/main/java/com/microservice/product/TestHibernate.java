package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.service.ProductService;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Etape 11 : verification de la configuration (conteneur Spring, SessionFactory, connexion JDBC).
 */
public class TestHibernate {

    public static void main(String[] args) throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            System.out.println("Contexte Spring initialise avec succes !");
            System.out.println("Nombre de beans declares : " + context.getBeanDefinitionCount());

            SessionFactory sessionFactory = context.getBean(SessionFactory.class);
            System.out.println("SessionFactory initialisee : " + (sessionFactory != null));
            System.out.println("Dialecte Hibernate           : "
                    + sessionFactory.unwrap(SessionFactoryImplementor.class)
                    .getJdbcServices().getDialect().getClass().getName());

            DataSource dataSource = context.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("Connexion JDBC operationnelle : "
                        + connection.getMetaData().getDatabaseProductName()
                        + " " + connection.getMetaData().getDatabaseProductVersion());
            }

            ProductService productService = context.getBean(ProductService.class);
            System.out.println("Produits presents en base    : " + productService.countProducts());
        } finally {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
