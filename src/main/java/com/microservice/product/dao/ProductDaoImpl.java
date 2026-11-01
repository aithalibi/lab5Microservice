package com.microservice.product.dao;

import com.microservice.product.entity.Product;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductDaoImpl implements IProductDao {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void save(Product product) {
        sessionFactory.getCurrentSession().save(product);
    }

    @Override
    public void update(Product product) {
        sessionFactory.getCurrentSession().update(product);
    }

    @Override
    public void delete(Product product) {
        sessionFactory.getCurrentSession().delete(product);
    }

    @Override
    public Product findById(Long id) {
        return sessionFactory.getCurrentSession().get(Product.class, id);
    }

    @Override
    public List<Product> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Product p ORDER BY p.id", Product.class)
                .list();
    }

    @Override
    public List<Product> findByName(String name) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Product p WHERE lower(p.name) = lower(:name) ORDER BY p.id", Product.class)
                .setParameter("name", name)
                .list();
    }

    @Override
    public long countAll() {
        Long total = sessionFactory.getCurrentSession()
                .createQuery("select count(p) from Product p", Long.class)
                .uniqueResult();
        return total == null ? 0L : total;
    }
}
