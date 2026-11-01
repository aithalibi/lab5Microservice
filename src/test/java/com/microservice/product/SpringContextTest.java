package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.entity.Product;
import com.microservice.product.service.ProductService;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Etape 11 : verifie que le conteneur Spring demarre, que la SessionFactory est
 * initialisee et que la connexion a la base est operationnelle.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
class SpringContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private HibernateTransactionManager transactionManager;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("Le conteneur Spring expose le DAO, le service et les beans de persistance")
    void leConteneurExposeLesBeansAttendus() {
        assertNotNull(context);
        assertNotNull(context.getBean(com.microservice.product.dao.IProductDao.class));
        assertNotNull(productService);
        assertTrue(context.containsBean("dataSource"));
        assertTrue(context.containsBean("sessionFactory"));
        assertTrue(context.containsBean("transactionManager"));
    }

    @Test
    @DisplayName("La SessionFactory Hibernate est configuree avec le dialecte declare")
    void laSessionFactoryEstConfiguree() {
        assertNotNull(sessionFactory);
        SessionFactoryImplementor implementor = sessionFactory.unwrap(SessionFactoryImplementor.class);
        assertEquals("org.hibernate.dialect.MySQL8Dialect",
                implementor.getJdbcServices().getDialect().getClass().getName());
        assertNotNull(sessionFactory.unwrap(EntityManagerFactory.class));
    }

    @Test
    @DisplayName("Le gestionnaire de transactions est lie a la SessionFactory")
    void leGestionnaireDeTransactionsEstLie() {
        assertNotNull(transactionManager);
        assertEquals(sessionFactory, transactionManager.getSessionFactory());
    }

    @Test
    @DisplayName("La connexion JDBC a la base product_db est operationnelle")
    void laConnexionJdbcEstOperationnelle() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2));
            assertEquals("product_db", connection.getCatalog());
            assertTrue(connection.getMetaData().getDatabaseProductName().contains("MySQL")
                    || connection.getMetaData().getDatabaseProductName().contains("MariaDB"));
        }
    }

    @Test
    @DisplayName("La table products est creee par hbm2ddl.auto=update")
    void laTableProductsEstCreee() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var st = connection.createStatement();
             var rs = st.executeQuery("select count(*) from products")) {
            assertTrue(rs.next());
            assertTrue(rs.getLong(1) >= 0);
        }
        assertNotNull(new Product("Sonde", 1));
    }
}
