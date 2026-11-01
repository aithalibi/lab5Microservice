package com.microservice.product.dao;

import com.microservice.product.entity.Product;

import java.util.List;

/**
 * Contrat specifique au produit : il herite du CRUD generique de {@link IDao} et
 * ajoute les requetes HQL propres au domaine metier.
 */
public interface IProductDao extends IDao<Product> {

    List<Product> findByName(String name);

    long countAll();
}
