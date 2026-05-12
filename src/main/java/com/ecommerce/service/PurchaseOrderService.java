package com.ecommerce.service;

import com.ecommerce.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseOrderService {
    List<PurchaseOrder> getVendorPurchaseOrders(Long vendorId);
    PurchaseOrder findById(Long id);
    PurchaseOrder createPurchaseOrder(Long vendorId, String supplierName, List<Long> variantIds, List<Integer> quantities, List<BigDecimal> unitCosts);
    void completePurchaseOrder(Long poId);
    void cancelPurchaseOrder(Long poId);
}
