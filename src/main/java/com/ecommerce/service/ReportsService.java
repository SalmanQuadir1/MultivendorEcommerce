package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportsService {
    // Sales Analytics
    BigDecimal getTotalRevenue();
    BigDecimal getRevenueByDateRange(LocalDate startDate, LocalDate endDate);
    List<Map<String, Object>> getRevenueByMonth(int year);
    List<Map<String, Object>> getTopSellingProducts(int limit);
    List<Map<String, Object>> getSalesByStatus();

    // Product Performance
    List<Map<String, Object>> getProductPerformanceReport();
    List<Map<String, Object>> getLowStockProducts();
    Map<String, Object> getInventorySummary();

    // Customer Analytics
    List<Map<String, Object>> getTopCustomers(int limit);
    Map<String, Object> getCustomerDemographics();
    List<Map<String, Object>> getCustomerOrderFrequency();

    // Vendor Analytics
    List<Map<String, Object>> getVendorPerformance();
    Map<String, Object> getVendorSalesSummary();

    // Time-based Reports
    List<Map<String, Object>> getDailySalesReport(LocalDate startDate, LocalDate endDate);
    List<Map<String, Object>> getMonthlySalesReport(int year);
    List<Map<String, Object>> getYearlySalesReport();

    // Category Analytics
    List<Map<String, Object>> getSalesByCategory();
    List<Map<String, Object>> getCategoryPerformance();

    // Order Analytics
    Map<String, Object> getOrderStatistics();
    List<Map<String, Object>> getOrdersByDateRange(LocalDate startDate, LocalDate endDate);
}