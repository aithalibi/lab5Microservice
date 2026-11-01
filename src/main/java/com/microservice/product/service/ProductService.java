package com.microservice.product.service;

import com.microservice.product.entity.Product;

import java.util.List;

public interface ProductService {

    void addProduct(Product product);

    void updateProduct(Product product);

    void deleteProduct(Product product);

    Product getProduct(Long id);

    List<Product> getAllProducts();

    List<Product> getProductsByName(String name);

    long countProducts();
}
