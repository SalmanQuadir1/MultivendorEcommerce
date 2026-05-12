package com.ecommerce.service;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    List<Product> findAll();
    Page<Product> findAll(Pageable pageable);
    Product findById(Long id);
    List<Product> findByVendorId(Long vendorId);
    Page<Product> findByVendorId(Long vendorId, Pageable pageable);
    List<Product> findByCategoryId(Long categoryId);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Product save(Product product);
    void deleteById(Long id);
}
