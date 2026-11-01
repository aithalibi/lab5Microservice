package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.entity.Product;
import com.microservice.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la couche service sur une vraie base MySQL. Chaque test s'execute dans une
 * transaction annotee : Spring l'annule a la fin, la base reste donc propre.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("addProduct enregistre le produit et genere son identifiant")
    void addProductEnregistreLeProduit() {
        Product produit = new Product("Spring Boot Starter", 39.90);

        productService.addProduct(produit);

        assertNotNull(produit.getId());
        Product enBase = productService.getProduct(produit.getId());
        assertNotNull(enBase);
        assertEquals("Spring Boot Starter", enBase.getName());
        assertEquals(39.90, enBase.getPrice(), 0.001);
    }

    @Test
    @DisplayName("addProduct refuse un prix negatif")
    void addProductRefuseUnPrixNegatif() {
        Product produit = new Product("Produit invalide", -1);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> productService.addProduct(produit));

        assertEquals("Le prix ne peut pas etre negatif", e.getMessage());
        assertNull(produit.getId());
    }

    @Test
    @DisplayName("addProduct refuse un nom vide")
    void addProductRefuseUnNomVide() {
        Product produit = new Product("   ", 12.5);

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(produit));
    }

    @Test
    @DisplayName("updateProduct met a jour le prix en base")
    void updateProductMetAJourLePrix() {
        Product produit = new Product("Microservice Gateway", 149.99);
        productService.addProduct(produit);

        produit.setPrice(199.99);
        productService.updateProduct(produit);

        assertEquals(199.99, productService.getProduct(produit.getId()).getPrice(), 0.001);
    }

    @Test
    @DisplayName("getAllProducts retourne les produits enregistres")
    void getAllProductsRetourneLesProduits() {
        productService.addProduct(new Product("Produit A", 10));
        productService.addProduct(new Product("Produit B", 20));

        List<Product> produits = productService.getAllProducts();

        assertEquals(2, produits.size());
        assertTrue(produits.stream().anyMatch(p -> "Produit A".equals(p.getName())));
        assertTrue(produits.stream().anyMatch(p -> "Produit B".equals(p.getName())));
    }

    @Test
    @DisplayName("deleteProduct supprime le produit")
    void deleteProductSupprimeLeProduit() {
        Product produit = new Product("Produit ephemere", 5);
        productService.addProduct(produit);
        Long id = produit.getId();

        productService.deleteProduct(produit);

        assertNull(productService.getProduct(id));
    }

    @Test
    @DisplayName("getProductsByName retrouve le produit par son nom (HQL parametre)")
    void getProductsByNameRetrouveLeProduit() {
        productService.addProduct(new Product("Api Gateway", 99));
        productService.addProduct(new Product("Autre produit", 15));

        List<Product> trouves = productService.getProductsByName("api gateway");

        assertEquals(1, trouves.size());
        assertEquals("Api Gateway", trouves.get(0).getName());
    }

    @Test
    @DisplayName("getProductsByName retourne une liste vide pour un nom inconnu")
    void getProductsByNameRetourneListeVide() {
        assertTrue(productService.getProductsByName("inexistant-" + System.nanoTime()).isEmpty());
    }

    @Test
    @DisplayName("getProduct retourne null pour un identifiant inconnu")
    void getProductRetourneNullSiInconnu() {
        assertNull(productService.getProduct(999_999L));
    }

    @Test
    @DisplayName("countProducts compte les produits presents")
    void countProductsCompteLesProduits() {
        assertEquals(0, productService.countProducts());

        productService.addProduct(new Product("Produit C", 30));

        assertEquals(1, productService.countProducts());
        assertFalse(productService.getAllProducts().isEmpty());
    }
}
