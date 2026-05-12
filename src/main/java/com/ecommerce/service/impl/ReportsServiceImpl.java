package com.ecommerce.service.impl;

import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportsServiceImpl implements ReportsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public BigDecimal getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt().isAfter(start) && order.getCreatedAt().isBefore(end))
                .filter(order -> order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<Map<String, Object>> getRevenueByMonth(int year) {
        List<Map<String, Object>> revenueByMonth = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            BigDecimal revenue = getRevenueByDateRange(startDate, endDate);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", startDate.format(DateTimeFormatter.ofPattern("MMM")));
            monthData.put("revenue", revenue);
            monthData.put("orders", getOrdersCountByDateRange(startDate, endDate));

            revenueByMonth.add(monthData);
        }

        return revenueByMonth;
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        return orderItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> product = new HashMap<>();
                    product.put("productName", entry.getKey());
                    product.put("totalSold", entry.getValue());
                    return product;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getSalesByStatus() {
        return Arrays.stream(OrderStatus.values())
                .map(status -> {
                    Map<String, Object> statusData = new HashMap<>();
                    long count = orderRepository.findAll().stream()
                            .filter(order -> order.getStatus() == status)
                            .count();
                    BigDecimal revenue = orderRepository.findAll().stream()
                            .filter(order -> order.getStatus() == status)
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    statusData.put("status", status.name());
                    statusData.put("count", count);
                    statusData.put("revenue", revenue);
                    return statusData;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getProductPerformanceReport() {
        return productRepository.findAllWithVariants().stream()
                .map(product -> {
                    Map<String, Object> report = new HashMap<>();
                    int totalSold = orderItemRepository.findAll().stream()
                            .filter(item -> item.getProduct().getId().equals(product.getId()))
                            .mapToInt(OrderItem::getQuantity)
                            .sum();

                    int totalStock = product.getVariants().stream()
                            .mapToInt(ProductVariant::getStock)
                            .sum();

                    BigDecimal totalRevenue = orderItemRepository.findAll().stream()
                            .filter(item -> item.getProduct().getId().equals(product.getId()))
                            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    report.put("productId", product.getId());
                    report.put("productName", product.getName());
                    report.put("category", product.getCategory() != null ? product.getCategory().getName() : "N/A");
                    report.put("totalSold", totalSold);
                    report.put("totalStock", totalStock);
                    report.put("totalRevenue", totalRevenue);
                    report.put("vendor", product.getVendor().getFullName());

                    return report;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("totalSold"), (Integer) a.get("totalSold")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getLowStockProducts() {
        return productRepository.findAllWithVariants().stream()
                .filter(product -> product.getVariants().stream().anyMatch(v -> v.getStock() != null && v.getStock() <= 10))
                .map(product -> {
                    Map<String, Object> lowStock = new HashMap<>();
                    lowStock.put("productId", product.getId());
                    lowStock.put("productName", product.getName());
                    lowStock.put("variants", product.getVariants().stream()
                            .filter(v -> v.getStock() != null && v.getStock() <= 10)
                            .map(v -> Map.of("size", v.getSize(), "stock", v.getStock()))
                            .collect(Collectors.toList()));
                    return lowStock;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getInventorySummary() {
        Map<String, Object> summary = new HashMap<>();

        long totalProducts = productRepository.count();
        long totalVariants = productVariantRepository.count();
        long lowStockVariants = productRepository.findAllWithVariants().stream()
                .flatMap(product -> product.getVariants().stream())
                .filter(v -> v.getStock() != null && v.getStock() <= 10)
                .count();
        long outOfStockVariants = productRepository.findAllWithVariants().stream()
                .flatMap(product -> product.getVariants().stream())
                .filter(v -> v.getStock() != null && v.getStock() == 0)
                .count();

        summary.put("totalProducts", totalProducts);
        summary.put("totalVariants", totalVariants);
        summary.put("lowStockVariants", lowStockVariants);
        summary.put("outOfStockVariants", outOfStockVariants);

        return summary;
    }

    @Override
    public List<Map<String, Object>> getTopCustomers(int limit) {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        order -> order.getUser().getFullName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                orders -> {
                                    BigDecimal totalSpent = orders.stream()
                                            .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.DELIVERED)
                                            .map(Order::getTotalAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return Map.of("totalSpent", totalSpent, "orderCount", orders.size());
                                }
                        )
                ))
                .entrySet().stream()
                .sorted((a, b) -> ((BigDecimal) b.getValue().get("totalSpent")).compareTo((BigDecimal) a.getValue().get("totalSpent")))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> customer = new HashMap<>();
                    customer.put("customerName", entry.getKey());
                    customer.put("totalSpent", entry.getValue().get("totalSpent"));
                    customer.put("orderCount", entry.getValue().get("orderCount"));
                    return customer;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCustomerDemographics() {
        List<User> customers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_CUSTOMER")))
                .collect(Collectors.toList());

        Map<String, Object> demographics = new HashMap<>();
        demographics.put("totalCustomers", customers.size());
        demographics.put("activeCustomers", customers.stream().filter(User::isEnabled).count());

        return demographics;
    }

    @Override
    public List<Map<String, Object>> getCustomerOrderFrequency() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        order -> order.getUser().getFullName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> frequency = new HashMap<>();
                    frequency.put("customerName", entry.getKey());
                    frequency.put("orderCount", entry.getValue());
                    return frequency;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getVendorPerformance() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_VENDOR")))
                .map(vendor -> {
                    Map<String, Object> performance = new HashMap<>();
                    List<OrderItem> vendorItems = orderItemRepository.findAll().stream()
                            .filter(item -> item.getVendor() != null && item.getVendor().getId().equals(vendor.getId()))
                            .collect(Collectors.toList());

                    BigDecimal totalRevenue = vendorItems.stream()
                            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int totalSold = vendorItems.stream().mapToInt(OrderItem::getQuantity).sum();
                    long productCount = productRepository.findByVendorId(vendor.getId()).size();

                    performance.put("vendorName", vendor.getFullName());
                    performance.put("totalRevenue", totalRevenue);
                    performance.put("totalSold", totalSold);
                    performance.put("productCount", productCount);

                    return performance;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("totalRevenue")).compareTo((BigDecimal) a.get("totalRevenue")))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getVendorSalesSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<User> vendors = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_VENDOR")))
                .collect(Collectors.toList());

        summary.put("totalVendors", vendors.size());
        summary.put("activeVendors", vendors.stream().filter(User::isEnabled).count());

        return summary;
    }

    @Override
    public List<Map<String, Object>> getDailySalesReport(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> dailyReport = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDate date = current;
            BigDecimal dailyRevenue = getRevenueByDateRange(date, date);
            long dailyOrders = getOrdersCountByDateRange(date, date);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dayData.put("revenue", dailyRevenue);
            dayData.put("orders", dailyOrders);

            dailyReport.add(dayData);
            current = current.plusDays(1);
        }

        return dailyReport;
    }

    @Override
    public List<Map<String, Object>> getMonthlySalesReport(int year) {
        return getRevenueByMonth(year);
    }

    @Override
    public List<Map<String, Object>> getYearlySalesReport() {
        List<Map<String, Object>> yearlyReport = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 4; year <= currentYear; year++) {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            BigDecimal yearlyRevenue = getRevenueByDateRange(startDate, endDate);

            Map<String, Object> yearData = new HashMap<>();
            yearData.put("year", year);
            yearData.put("revenue", yearlyRevenue);
            yearData.put("orders", getOrdersCountByDateRange(startDate, endDate));

            yearlyReport.add(yearData);
        }

        return yearlyReport;
    }

    @Override
    public List<Map<String, Object>> getSalesByCategory() {
        return categoryRepository.findAll().stream()
                .map(category -> {
                    Map<String, Object> categoryData = new HashMap<>();
                    List<Product> categoryProducts = productRepository.findByCategoryId(category.getId());

                    BigDecimal categoryRevenue = orderItemRepository.findAll().stream()
                            .filter(item -> categoryProducts.stream()
                                    .anyMatch(p -> p.getId().equals(item.getProduct().getId())))
                            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int categorySales = orderItemRepository.findAll().stream()
                            .filter(item -> categoryProducts.stream()
                                    .anyMatch(p -> p.getId().equals(item.getProduct().getId())))
                            .mapToInt(OrderItem::getQuantity)
                            .sum();

                    categoryData.put("categoryName", category.getName());
                    categoryData.put("revenue", categoryRevenue);
                    categoryData.put("sales", categorySales);
                    categoryData.put("productCount", categoryProducts.size());

                    return categoryData;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCategoryPerformance() {
        return getSalesByCategory();
    }

    @Override
    public Map<String, Object> getOrderStatistics() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", orders.size());
        stats.put("pendingOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        stats.put("paidOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count());
        stats.put("shippedOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count());
        stats.put("deliveredOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count());
        stats.put("cancelledOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());

        return stats;
    }

    @Override
    public List<Map<String, Object>> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt().isAfter(start) && order.getCreatedAt().isBefore(end))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(order -> {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("orderId", order.getId());
                    orderData.put("customerName", order.getUser().getFullName());
                    orderData.put("totalAmount", order.getTotalAmount());
                    orderData.put("status", order.getStatus().name());
                    orderData.put("createdAt", order.getCreatedAt());
                    orderData.put("itemCount", order.getItems().size());
                    return orderData;
                })
                .collect(Collectors.toList());
    }

    // Helper method
    private long getOrdersCountByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt().isAfter(start) && order.getCreatedAt().isBefore(end))
                .count();
    }
}