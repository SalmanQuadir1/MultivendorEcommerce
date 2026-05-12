package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String size;

    private String sku;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private java.math.BigDecimal price;

    @Column(name = "unit_price", nullable = false)
    private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;
}
