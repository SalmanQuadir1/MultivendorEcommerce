package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usedCount;

    private LocalDate validFrom;

    private LocalDate validUntil;

    @ManyToMany
    @JoinTable(name = "coupon_products",
               joinColumns = @JoinColumn(name = "coupon_id"),
               inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> applicableProducts = new ArrayList<>();

    @Transient
    private List<Long> selectedProductIds;

    private boolean active = true;

    private String description;

    public enum DiscountType {
        PERCENTAGE, FLAT
    }

    public boolean isValid() {
        if (!active) return false;
        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) return false;
        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) return false;
        if (validUntil != null && today.isAfter(validUntil)) return false;
        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal applicableTotal) {
        if (!isValid()) return BigDecimal.ZERO;
        if (minOrderAmount != null && applicableTotal.compareTo(minOrderAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = applicableTotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount;
    }
}
