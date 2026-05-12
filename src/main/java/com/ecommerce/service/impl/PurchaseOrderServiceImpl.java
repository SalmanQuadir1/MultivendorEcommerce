package com.ecommerce.service.impl;

import com.ecommerce.entity.*;
import com.ecommerce.repository.InventoryTransactionRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.PurchaseOrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    public List<PurchaseOrder> getVendorPurchaseOrders(Long vendorId) {
        return purchaseOrderRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    @Override
    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id).orElse(null);
    }

    @Override
    public PurchaseOrder createPurchaseOrder(Long vendorId, String supplierName, List<Long> variantIds, List<Integer> quantities, List<BigDecimal> unitCosts) {
        User vendor = userRepository.findById(vendorId).orElseThrow(() -> new RuntimeException("Vendor not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setVendor(vendor);
        po.setSupplierName(supplierName);
        po.setStatus(PurchaseOrderStatus.PENDING);
        po.setPoNumber("PO-" + System.currentTimeMillis() % 10000000 + new Random().nextInt(10));

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();

        for (int i = 0; i < variantIds.size(); i++) {
            ProductVariant variant = productVariantRepository.findById(variantIds.get(i)).orElseThrow(() -> new RuntimeException("Variant not found"));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setVariant(variant);
            item.setQuantity(quantities.get(i));
            item.setUnitCost(unitCosts.get(i));
            items.add(item);

            total = total.add(unitCosts.get(i).multiply(new BigDecimal(quantities.get(i))));
        }

        po.setItems(items);
        po.setTotalAmount(total);

        return purchaseOrderRepository.save(po);
    }

    @Override
    public void completePurchaseOrder(Long poId) {
        PurchaseOrder po = findById(poId);
        if (po != null && po.getStatus() == PurchaseOrderStatus.PENDING) {
            po.setStatus(PurchaseOrderStatus.COMPLETED);
            po.setCompletedAt(LocalDateTime.now());

            for (PurchaseOrderItem item : po.getItems()) {
                ProductVariant variant = item.getVariant();
                variant.setStock(variant.getStock() + item.getQuantity());
                productVariantRepository.save(variant);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setVariant(variant);
                tx.setTransactionType(InventoryTransactionType.PURCHASE_ORDER);
                tx.setQuantityChange(item.getQuantity());
                tx.setReference("Purchase Order " + po.getPoNumber());
                inventoryTransactionRepository.save(tx);
            }

            purchaseOrderRepository.save(po);
        }
    }

    @Override
    public void cancelPurchaseOrder(Long poId) {
        PurchaseOrder po = findById(poId);
        if (po != null && po.getStatus() == PurchaseOrderStatus.PENDING) {
            po.setStatus(PurchaseOrderStatus.CANCELLED);
            purchaseOrderRepository.save(po);
        }
    }
}
