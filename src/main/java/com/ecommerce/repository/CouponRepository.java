package com.ecommerce.repository;

import com.ecommerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.active = true AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit) AND (c.validUntil IS NULL OR c.validUntil >= :today) AND (c.validFrom IS NULL OR c.validFrom <= :today)")
    List<Coupon> findActiveValidCoupons(@Param("today") LocalDate today);
}
