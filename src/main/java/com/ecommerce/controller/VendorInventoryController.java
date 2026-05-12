package com.ecommerce.controller;

import com.ecommerce.entity.*;
import com.ecommerce.repository.InventoryTransactionRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor/inventory")
@RequiredArgsConstructor
public class VendorInventoryController {

    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final com.ecommerce.repository.PriceHistoryLogRepository priceHistoryLogRepository;
    private final com.ecommerce.repository.UserRepository userRepository;

    @GetMapping
    public String listInventory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Product> products = productService.findByVendorId(userDetails.getId());
        List<ProductVariant> variants = products.stream()
                .flatMap(p -> p.getVariants().stream())
                .collect(Collectors.toList());
        model.addAttribute("variants", variants);
        return "vendor/inventory/list";
    }

    @PostMapping("/update/{id}")
    public String updateStock(@PathVariable("id") Long variantId, 
                              @RequestParam("stock") Integer stock, 
                              @RequestParam("price") BigDecimal price,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant != null && variant.getProduct().getVendor().getId().equals(userDetails.getId())) {
            
            // Log Price Change History if price is modified
            if (price != null && (variant.getPrice() == null || price.compareTo(variant.getPrice()) != 0)) {
                User user = userRepository.findById(userDetails.getId()).orElse(null);
                if (user != null) {
                    PriceHistoryLog log = new PriceHistoryLog();
                    log.setVariant(variant);
                    log.setOldPrice(variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO);
                    log.setNewPrice(price);
                    log.setChangedBy(user);
                    priceHistoryLogRepository.save(log);
                }
                variant.setPrice(price);
            }

            // Log Stock Change Transaction
            int diff = stock - variant.getStock();
            if (diff != 0) {
                variant.setStock(stock);
                
                InventoryTransaction tx = new InventoryTransaction();
                tx.setVariant(variant);
                tx.setTransactionType(InventoryTransactionType.MANUAL_UPDATE);
                tx.setQuantityChange(diff);
                tx.setReference("Manual Adjustment");
                inventoryTransactionRepository.save(tx);
            }
            
            productVariantRepository.save(variant);
        }
        return "redirect:/vendor/inventory";
    }

    @GetMapping("/low-stock")
    public String listLowStock(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Product> products = productService.findByVendorId(userDetails.getId());
        List<ProductVariant> lowStockVariants = products.stream()
                .flatMap(p -> p.getVariants().stream())
                .filter(v -> v.getStock() != null && v.getStock() <= 10)
                .collect(Collectors.toList());
        model.addAttribute("variants", lowStockVariants);
        return "vendor/inventory/low-stock";
    }

    @GetMapping("/history")
    public String viewHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByVariantProductVendorIdOrderByCreatedAtDesc(userDetails.getId());
        List<PriceHistoryLog> priceLogs = priceHistoryLogRepository.findByVariantProductVendorIdOrderByChangedAtDesc(userDetails.getId());
        model.addAttribute("transactions", transactions);
        model.addAttribute("priceLogs", priceLogs);
        return "vendor/inventory/history";
    }

    @GetMapping("/purchase-orders")
    public String listPurchaseOrders(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<PurchaseOrder> orders = purchaseOrderService.getVendorPurchaseOrders(userDetails.getId());
        model.addAttribute("orders", orders);
        return "vendor/inventory/po-list";
    }

    @GetMapping("/purchase-orders/new")
    public String showCreatePoForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<ProductVariant> variants = productVariantRepository.findByProductVendorId(userDetails.getId());
        model.addAttribute("variants", variants);
        return "vendor/inventory/po-form";
    }

    @PostMapping("/purchase-orders/save")
    public String createPurchaseOrder(
            @RequestParam("supplierName") String supplierName,
            @RequestParam("variantIds") List<Long> variantIds,
            @RequestParam("quantities") List<Integer> quantities,
            @RequestParam("unitCosts") List<BigDecimal> unitCosts,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        purchaseOrderService.createPurchaseOrder(userDetails.getId(), supplierName, variantIds, quantities, unitCosts);
        return "redirect:/vendor/inventory/purchase-orders";
    }

    @PostMapping("/purchase-orders/complete/{id}")
    public String completePo(@PathVariable("id") Long id) {
        purchaseOrderService.completePurchaseOrder(id);
        return "redirect:/vendor/inventory/purchase-orders";
    }

    @PostMapping("/purchase-orders/cancel/{id}")
    public String cancelPo(@PathVariable("id") Long id) {
        purchaseOrderService.cancelPurchaseOrder(id);
        return "redirect:/vendor/inventory/purchase-orders";
    }
}
