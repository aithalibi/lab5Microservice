package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.entity.Product;
import com.microservice.product.service.ProductService;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Etape 12 : scenario d'utilisation complet (service -> DAO -> Hibernate -> MySQL).
 */
public class MainApp {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            ProductService productService = context.getBean(ProductService.class);

            Product produit = new Product("Microservice Gateway", 149.99);
            productService.addProduct(produit);

            System.out.println("Produit ajoute avec succes ! Identifiant genere : " + produit.getId());
            System.out.println("Total en base : " + productService.countProducts());

            System.out.println("--- Liste des produits ---");
            for (Product p : productService.getAllProducts()) {
                System.out.println(p);
            }

            produit.setPrice(199.99);
            productService.updateProduct(produit);
            System.out.println("Prix mis a jour : " + productService.getProduct(produit.getId()).getPrice());

            System.out.println("--- Test du rollback transactionnel ---");
            try {
                productService.addProduct(new Product("Produit invalide", -10));
            } catch (IllegalArgumentException e) {
                System.out.println("Produit refuse par la couche service : " + e.getMessage());
            }
            System.out.println("Total en base apres le refus : " + productService.countProducts());
        } finally {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
