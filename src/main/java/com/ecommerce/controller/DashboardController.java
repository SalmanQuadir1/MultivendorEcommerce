package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReportsService;
import com.ecommerce.service.JasperReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProductService productService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReportsService reportsService;
    private final JasperReportService jasperReportService;
    private final com.ecommerce.repository.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @RequestParam(defaultValue = "0") int ordersPage,
            @RequestParam(defaultValue = "0") int productsPage,
            @RequestParam(defaultValue = "0") int usersPage,
            @RequestParam(defaultValue = "5") int ordersSize,
            @RequestParam(defaultValue = "5") int productsSize,
            @RequestParam(defaultValue = "5") int usersSize,
            Model model) {

        // Create pageable objects for each tab
        Pageable ordersPageable = PageRequest.of(ordersPage, ordersSize, Sort.by("createdAt").descending());
        Pageable productsPageable = PageRequest.of(productsPage, productsSize, Sort.by("id").descending());
        Pageable usersPageable = PageRequest.of(usersPage, usersSize, Sort.by("id").descending());

        // Get paginated data
        Page<Order> ordersPageResult = orderRepository.findAllWithItems(ordersPageable);
        Page<Product> productsPageResult = productService.findAll(productsPageable);
        Page<User> usersPageResult = userRepository.findAll(usersPageable);

        // Basic statistics (use all data for stats, not paginated)
        List<Product> allProducts = productService.findAll();
        List<Order> allOrders = orderRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        // Enhanced analytics using ReportsService
        BigDecimal totalRevenue = reportsService.getTotalRevenue();
        var salesByMonth = reportsService.getRevenueByMonth(LocalDate.now().getYear());
        var topSellingProducts = reportsService.getTopSellingProducts(10);
        var salesByStatus = reportsService.getSalesByStatus();
        var productPerformance = reportsService.getProductPerformanceReport();
        var topCustomers = reportsService.getTopCustomers(10);
        var vendorPerformance = reportsService.getVendorPerformance();
        var salesByCategory = reportsService.getSalesByCategory();
        var orderStatistics = reportsService.getOrderStatistics();
        var inventorySummary = reportsService.getInventorySummary();

        long totalProducts = allProducts.size();
        long totalOrders = allOrders.size();
        long totalUsers = allUsers.size();
        long totalVendors = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VENDOR"))).count();

        long lowStockCount = (Long) inventorySummary.get("lowStockVariants");

        // Add all data to model
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalVendors", totalVendors);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("lowStockCount", lowStockCount);

        // Paginated data
        model.addAttribute("orders", ordersPageResult);
        model.addAttribute("products", productsPageResult);
        model.addAttribute("users", usersPageResult);

        // Enhanced analytics data
        model.addAttribute("salesByMonth", salesByMonth);
        model.addAttribute("topSellingProducts", topSellingProducts);
        model.addAttribute("salesByStatus", salesByStatus);
        model.addAttribute("productPerformance", productPerformance);
        model.addAttribute("topCustomers", topCustomers);
        model.addAttribute("vendorPerformance", vendorPerformance);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("orderStatistics", orderStatistics);
        model.addAttribute("inventorySummary", inventorySummary);

        return "admin/dashboard";
    }

    @PostMapping("/admin/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String adminProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> productsPage = productService.findAll(pageable);
        model.addAttribute("products", productsPage);
        return "admin/products";
    }

    @GetMapping("/admin/users")
    public String adminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> usersPage = userRepository.findAll(pageable);
        model.addAttribute("users", usersPage);
        return "admin/users";
    }

    @GetMapping("/admin/vendors")
    public String adminVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        
        List<User> allVendors = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VENDOR")))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allVendors.size());
        List<User> pageContent = new ArrayList<>();
        if (start < allVendors.size()) {
            pageContent = allVendors.subList(start, end);
        }
        Page<User> pagedVendors = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allVendors.size());

        Map<Long, Map<String, Object>> vendorStats = new HashMap<>();
        for (User v : allVendors) {
            Map<String, Object> stats = new HashMap<>();
            long productsCount = productService.findByVendorId(v.getId()).size();
            BigDecimal totalSales = orderService.findVendorOrderItems(v.getId()).stream()
                    .map(item -> {
                        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                        return price.multiply(new BigDecimal(item.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("productsCount", productsCount);
            stats.put("totalSales", totalSales);
            vendorStats.put(v.getId(), stats);
        }

        long activeVendorsCount = allVendors.stream().filter(User::isEnabled).count();
        long suspendedVendorsCount = allVendors.stream().filter(u -> !u.isEnabled()).count();

        model.addAttribute("vendors", pagedVendors);
        model.addAttribute("vendorStats", vendorStats);
        model.addAttribute("activeVendorsCount", activeVendorsCount);
        model.addAttribute("suspendedVendorsCount", suspendedVendorsCount);
        return "admin/vendors";
    }

    @PostMapping("/admin/vendors/toggle/{id}")
    public String toggleVendorStatus(@PathVariable("id") Long vendorId) {
        User vendor = userRepository.findById(vendorId).orElse(null);
        if (vendor != null) {
            vendor.setEnabled(!vendor.isEnabled());
            userRepository.save(vendor);
        }
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/vendors/create")
    public String createVendor(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String password) {
        if (!userRepository.existsByEmail(email)) {
            User vendor = new User();
            vendor.setEmail(email);
            vendor.setFullName(fullName);
            vendor.setPassword(passwordEncoder.encode(password));
            vendor.setEnabled(true);
            
            com.ecommerce.entity.Role vendorRole = roleRepository.findByName("ROLE_VENDOR").orElseThrow();
            vendor.setRoles(java.util.Collections.singleton(vendorRole));
            userRepository.save(vendor);
        }
        return "redirect:/admin/vendors";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        // Basic data
        List<Product> products = productService.findAll();
        List<Order> orders = orderRepository.findAll();
        List<User> users = userRepository.findAll();

        // Enhanced analytics using ReportsService
        BigDecimal totalRevenue = reportsService.getTotalRevenue();
        var salesByMonth = reportsService.getRevenueByMonth(LocalDate.now().getYear());
        var topSellingProducts = reportsService.getTopSellingProducts(10);
        var salesByStatus = reportsService.getSalesByStatus();
        var productPerformance = reportsService.getProductPerformanceReport();
        var topCustomers = reportsService.getTopCustomers(10);
        var vendorPerformance = reportsService.getVendorPerformance();
        var salesByCategory = reportsService.getSalesByCategory();
        var orderStatistics = reportsService.getOrderStatistics();
        var inventorySummary = reportsService.getInventorySummary();

        long totalVendors = users.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VENDOR"))).count();

        long lowStockCount = (Long) inventorySummary.get("lowStockVariants");

        // Add all data to model
        model.addAttribute("orders", orders);
        model.addAttribute("products", products);
        model.addAttribute("users", users);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("totalVendors", totalVendors);

        // Enhanced analytics data
        model.addAttribute("salesByMonth", salesByMonth);
        model.addAttribute("topSellingProducts", topSellingProducts);
        model.addAttribute("salesByStatus", salesByStatus);
        model.addAttribute("productPerformance", productPerformance);
        model.addAttribute("topCustomers", topCustomers);
        model.addAttribute("vendorPerformance", vendorPerformance);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("orderStatistics", orderStatistics);
        model.addAttribute("inventorySummary", inventorySummary);

        return "admin/reports";
    }

    @GetMapping("/admin/reports/top-products/download")
    public ResponseEntity<byte[]> downloadTopProductsReport() {
        try {
            var topSellingProducts = reportsService.getTopSellingProducts(50);
            List<Map<String, String>> dataList = new ArrayList<>();
            for (var p : topSellingProducts) {
                Map<String, String> row = new HashMap<>();
                row.put("productName", String.valueOf(p.get("productName")));
                row.put("totalSold", String.valueOf(p.get("totalSold")));
                dataList.add(row);
            }

            String[] headers = {"Product Name", "Units Sold"};
            String[] fields = {"productName", "totalSold"};

            byte[] excelBytes = jasperReportService.generateExcelReport("Top Selling Products Report", headers, fields, dataList);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            responseHeaders.setContentDispositionFormData("attachment", "top_selling_products_report.xlsx");

            return new ResponseEntity<>(excelBytes, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/admin/reports/top-customers/download")
    public ResponseEntity<byte[]> downloadTopCustomersReport() {
        try {
            var topCustomers = reportsService.getTopCustomers(50);
            List<Map<String, String>> dataList = new ArrayList<>();
            for (var c : topCustomers) {
                Map<String, String> row = new HashMap<>();
                row.put("customerName", String.valueOf(c.get("customerName")));
                row.put("totalSpent", "INR " + String.valueOf(c.get("totalSpent")));
                row.put("orderCount", String.valueOf(c.get("orderCount")));
                dataList.add(row);
            }

            String[] headers = {"Customer Name", "Total Spent", "Total Orders"};
            String[] fields = {"customerName", "totalSpent", "orderCount"};

            byte[] excelBytes = jasperReportService.generateExcelReport("Top Customers Performance Report", headers, fields, dataList);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            responseHeaders.setContentDispositionFormData("attachment", "top_customers_report.xlsx");

            return new ResponseEntity<>(excelBytes, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/admin/reports/categories/download")
    public ResponseEntity<byte[]> downloadCategoryPerformanceReport() {
        try {
            var salesByCategory = reportsService.getSalesByCategory();
            List<Map<String, String>> dataList = new ArrayList<>();
            for (var c : salesByCategory) {
                Map<String, String> row = new HashMap<>();
                row.put("categoryName", String.valueOf(c.get("categoryName")));
                row.put("revenue", "INR " + String.valueOf(c.get("revenue")));
                row.put("sales", String.valueOf(c.get("sales")));
                row.put("productCount", String.valueOf(c.get("productCount")));
                dataList.add(row);
            }

            String[] headers = {"Category Name", "Total Revenue", "Units Sold", "Product Count"};
            String[] fields = {"categoryName", "revenue", "sales", "productCount"};

            byte[] excelBytes = jasperReportService.generateExcelReport("Category Performance Report", headers, fields, dataList);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            responseHeaders.setContentDispositionFormData("attachment", "category_performance_report.xlsx");

            return new ResponseEntity<>(excelBytes, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/vendor/dashboard")
    public String vendorDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long vendorId = userDetails.getId();

        List<Product> products = productService.findByVendorId(vendorId);
        List<OrderItem> orderItems = orderService.findVendorOrderItems(vendorId);

        long totalProducts = products.size();
        long lowStockCount = products.stream()
                .filter(p -> p.getVariants().stream().anyMatch(v -> v.getStock() != null && v.getStock() <= 10))
                .count();

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("products", products);
        model.addAttribute("orderItems", orderItems);

        return "vendor/dashboard";
    }
}
