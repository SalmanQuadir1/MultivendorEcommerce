package com.ecommerce.repository;

import com.ecommerce.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    boolean existsByPoNumber(String poNumber);
}
