package com.ecommerce.controller;

import com.ecommerce.entity.*;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.PurchaseOrderRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.JasperReportService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor/reports/pl")
@RequiredArgsConstructor
public class VendorReportsController {

    private final OrderItemRepository orderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final JasperReportService jasperReportService;

    @GetMapping
    public String getProfitAndLossReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long vendorId = userDetails.getId();
        List<OrderItem> orderItems = orderItemRepository.findByVendorId(vendorId);

        // Fetch all completed purchase orders for this vendor to determine cost prices
        List<PurchaseOrder> completedPOs = purchaseOrderRepository.findByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.COMPLETED)
                .collect(Collectors.toList());

        // Filter order items based on date and product name/SKU search
        List<PLItem> plItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO;

        for (OrderItem item : orderItems) {
            Order order = item.getOrder();
            if (order == null) continue;

            LocalDateTime orderDate = order.getCreatedAt();
            if (orderDate == null) continue;

            // Date filtering
            if (startDate != null && orderDate.isBefore(startDate.atStartOfDay())) {
                continue;
            }
            if (endDate != null && orderDate.isAfter(endDate.atTime(LocalTime.MAX))) {
                continue;
            }

            // Keyword filtering
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.trim().toLowerCase();
                boolean matchesProduct = item.getProduct() != null && item.getProduct().getName().toLowerCase().contains(searchLower);
                boolean matchesSku = item.getVariant() != null && item.getVariant().getSku() != null && item.getVariant().getSku().toLowerCase().contains(searchLower);
                if (!matchesProduct && !matchesSku) {
                    continue;
                }
            }

            // Determine unit selling price directly from Product/Variant to P&L
            BigDecimal unitPrice = BigDecimal.ZERO;
            if (item.getVariant() != null && item.getVariant().getPrice() != null) {
                unitPrice = item.getVariant().getPrice();
            } else if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                unitPrice = item.getProduct().getPrice();
            } else if (item.getPrice() != null) {
                unitPrice = item.getPrice();
            }

            // Determine unit cost
            BigDecimal unitCost = BigDecimal.ZERO;
            boolean costFound = false;

            // Look up latest unit cost from completed POs for this variant
            if (item.getVariant() != null) {
                Long variantId = item.getVariant().getId();
                for (PurchaseOrder po : completedPOs) {
                    for (PurchaseOrderItem poItem : po.getItems()) {
                        if (poItem.getVariant() != null && poItem.getVariant().getId().equals(variantId)) {
                            unitCost = poItem.getUnitCost();
                            costFound = true;
                            break;
                        }
                    }
                    if (costFound) break;
                }
            }

            // Fallback: use variant's unitPrice if set, else 60% of selling price
            if (!costFound) {
                if (item.getVariant() != null && item.getVariant().getUnitPrice() != null && item.getVariant().getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                    unitCost = item.getVariant().getUnitPrice();
                } else if (unitPrice != null) {
                    unitCost = unitPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
                }
            }

            BigDecimal revenue = unitPrice.multiply(new BigDecimal(item.getQuantity()));
            BigDecimal cogs = unitCost.multiply(new BigDecimal(item.getQuantity()));
            BigDecimal profit = revenue.subtract(cogs);

            PLItem plItem = new PLItem();
            plItem.setTimestamp(orderDate);
            plItem.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Deleted Product");
            plItem.setSize(item.getVariant() != null ? item.getVariant().getSize() : "N/A");
            plItem.setSku(item.getVariant() != null ? item.getVariant().getSku() : "N/A");
            plItem.setQuantity(item.getQuantity());
            plItem.setPrice(unitPrice);
            plItem.setRevenue(revenue);
            plItem.setUnitCost(unitCost);
            plItem.setCogs(cogs);
            plItem.setProfit(profit);

            plItems.add(plItem);
            totalRevenue = totalRevenue.add(revenue);
            totalCOGS = totalCOGS.add(cogs);
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCOGS);
        BigDecimal netProfitMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            netProfitMargin = grossProfit.multiply(new BigDecimal("100"))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        // Paginate filtered items list
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), plItems.size());
        
        List<PLItem> pageContent = new ArrayList<>();
        if (start < plItems.size()) {
            pageContent = plItems.subList(start, end);
        }
        
        org.springframework.data.domain.Page<PLItem> pagedPLItems = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, plItems.size()
        );

        model.addAttribute("plItems", pagedPLItems);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCOGS", totalCOGS);
        model.addAttribute("grossProfit", grossProfit);
        model.addAttribute("netProfitMargin", netProfitMargin);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);

        return "vendor/reports/pl";
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadExcelReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long vendorId = userDetails.getId();
            List<OrderItem> orderItems = orderItemRepository.findByVendorId(vendorId);

            List<PurchaseOrder> completedPOs = purchaseOrderRepository.findByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                    .filter(po -> po.getStatus() == PurchaseOrderStatus.COMPLETED)
                    .collect(Collectors.toList());

            List<Map<String, String>> dataList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            for (OrderItem item : orderItems) {
                Order order = item.getOrder();
                if (order == null) continue;

                LocalDateTime orderDate = order.getCreatedAt();
                if (orderDate == null) continue;

                // Date filtering
                if (startDate != null && orderDate.isBefore(startDate.atStartOfDay())) continue;
                if (endDate != null && orderDate.isAfter(endDate.atTime(LocalTime.MAX))) continue;

                // Keyword filtering
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.trim().toLowerCase();
                    boolean matchesProduct = item.getProduct() != null && item.getProduct().getName().toLowerCase().contains(searchLower);
                    boolean matchesSku = item.getVariant() != null && item.getVariant().getSku() != null && item.getVariant().getSku().toLowerCase().contains(searchLower);
                    if (!matchesProduct && !matchesSku) continue;
                }

                // Determine unit selling price directly from Product/Variant to P&L
                BigDecimal unitPrice = BigDecimal.ZERO;
                if (item.getVariant() != null && item.getVariant().getPrice() != null) {
                    unitPrice = item.getVariant().getPrice();
                } else if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                    unitPrice = item.getProduct().getPrice();
                } else if (item.getPrice() != null) {
                    unitPrice = item.getPrice();
                }

                // Determine unit cost
                BigDecimal unitCost = BigDecimal.ZERO;
                boolean costFound = false;

                if (item.getVariant() != null) {
                    Long variantId = item.getVariant().getId();
                    for (PurchaseOrder po : completedPOs) {
                        for (PurchaseOrderItem poItem : po.getItems()) {
                            if (poItem.getVariant() != null && poItem.getVariant().getId().equals(variantId)) {
                                unitCost = poItem.getUnitCost();
                                costFound = true;
                                break;
                            }
                        }
                        if (costFound) break;
                    }
                }

                if (!costFound) {
                    if (item.getVariant() != null && item.getVariant().getUnitPrice() != null && item.getVariant().getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                        unitCost = item.getVariant().getUnitPrice();
                    } else if (unitPrice != null) {
                        unitCost = unitPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                BigDecimal revenue = unitPrice.multiply(new BigDecimal(item.getQuantity()));
                BigDecimal cogs = unitCost.multiply(new BigDecimal(item.getQuantity()));
                BigDecimal profit = revenue.subtract(cogs);

                Map<String, String> row = new HashMap<>();
                row.put("date", orderDate.format(formatter));
                row.put("productName", item.getProduct() != null ? item.getProduct().getName() : "Deleted Product");
                row.put("sku", item.getVariant() != null ? item.getVariant().getSku() : "N/A");
                row.put("quantity", String.valueOf(item.getQuantity()));
                row.put("price", "INR " + unitPrice);
                row.put("revenue", "INR " + revenue);
                row.put("unitCost", "INR " + unitCost);
                row.put("cogs", "INR " + cogs);
                row.put("profit", "INR " + profit);
                dataList.add(row);
            }

            String[] headers = {"Date & Time", "Product Name", "SKU", "Qty", "Selling Price", "Revenue", "Unit Cost", "COGS", "Gross Profit"};
            String[] fields = {"date", "productName", "sku", "quantity", "price", "revenue", "unitCost", "cogs", "profit"};

            byte[] excelBytes = jasperReportService.generateExcelReport("Profit & Loss Financial Statement", headers, fields, dataList);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            responseHeaders.setContentDispositionFormData("attachment", "profit_loss_report.xlsx");

            return new ResponseEntity<>(excelBytes, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Getter
    @Setter
    public static class PLItem {
        private LocalDateTime timestamp;
        private String productName;
        private String size;
        private String sku;
        private int quantity;
        private BigDecimal price;
        private BigDecimal revenue;
        private BigDecimal unitCost;
        private BigDecimal cogs;
        private BigDecimal profit;
    }
}
