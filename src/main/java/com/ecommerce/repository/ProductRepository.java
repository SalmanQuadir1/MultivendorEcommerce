package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendorId(Long vendorId);
    Page<Product> findByVendorId(Long vendorId, Pageable pageable);
    List<Product> findByCategoryId(Long categoryId);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.vendor.id = :vendorId")
    List<Product> findByVendorIdWithVariants(@Param("vendorId") Long vendorId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants")
    List<Product> findAllWithVariants();

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.variants",
           countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithVariants(Pageable pageable);

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.category.id = :categoryId",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryIdWithVariants(@Param("categoryId") Long categoryId, Pageable pageable);

    // ── Full Search: keyword + optional category + optional price range ──────
    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE " +
                   "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                   "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                   "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                   "(:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                        "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                  @Param("categoryId") Long categoryId,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice,
                                  Pageable pageable);
}

