package com.microservice.product.service;

import com.microservice.product.dao.IProductDao;
import com.microservice.product.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final IProductDao productDao;

    @Autowired
    public ProductServiceImpl(IProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    @Transactional
    public void addProduct(Product product) {
        validate(product);
        productDao.save(product);
    }

    @Override
    @Transactional
    public void updateProduct(Product product) {
        validate(product);
        if (product.getId() == null) {
            throw new IllegalArgumentException("Impossible de mettre a jour un produit sans identifiant");
        }
        productDao.update(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Impossible de supprimer un produit sans identifiant");
        }
        productDao.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByName(String name) {
        return productDao.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts() {
        return productDao.countAll();
    }

    private void validate(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Le produit ne peut pas etre null");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas etre negatif");
        }
    }
}
