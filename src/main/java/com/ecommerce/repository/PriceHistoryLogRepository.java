package com.ecommerce.repository;

import com.ecommerce.entity.PriceHistoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceHistoryLogRepository extends JpaRepository<PriceHistoryLog, Long> {
    List<PriceHistoryLog> findByVariantProductIdOrderByChangedAtDesc(Long productId);
    List<PriceHistoryLog> findByVariantIdOrderByChangedAtDesc(Long variantId);
    List<PriceHistoryLog> findByVariantProductVendorIdOrderByChangedAtDesc(Long vendorId);
}
