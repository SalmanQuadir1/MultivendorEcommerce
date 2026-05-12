package com.ecommerce.config;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.ecommerce.repository.OrderRepository orderRepository;
    private final com.ecommerce.repository.PaymentRepository paymentRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE products DROP COLUMN stock");
            System.out.println("====== Dropped obsolete 'stock' column from products table ======");
        } catch (Exception e) {
            // Column already dropped or doesn't exist
        }

        // Clean up existing data to replace with Dryfruit catalog!
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE order_items");
            jdbcTemplate.execute("TRUNCATE TABLE orders");
            jdbcTemplate.execute("TRUNCATE TABLE payments");
            jdbcTemplate.execute("TRUNCATE TABLE cart_items");
            jdbcTemplate.execute("TRUNCATE TABLE carts");
            jdbcTemplate.execute("TRUNCATE TABLE product_variants");
            jdbcTemplate.execute("TRUNCATE TABLE products");
            jdbcTemplate.execute("TRUNCATE TABLE categories");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("====== Cleared existing products, categories, variants, and order histories successfully ======");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("DELETE FROM order_items");
                jdbcTemplate.execute("DELETE FROM orders");
                jdbcTemplate.execute("DELETE FROM payments");
                jdbcTemplate.execute("DELETE FROM cart_items");
                jdbcTemplate.execute("DELETE FROM carts");
                jdbcTemplate.execute("DELETE FROM product_variants");
                jdbcTemplate.execute("DELETE FROM products");
                jdbcTemplate.execute("DELETE FROM categories");
                System.out.println("====== Cleared existing data via DELETE ======");
            } catch (Exception ex) {
                System.out.println("====== Data cleanup skipped/failed: " + ex.getMessage() + " ======");
            }
        }

        seedRoles();
        seedUsers();
        seedProducts();
        seedOrdersAndPayments();
    }

    private void seedRoles() {
        String[] roleNames = {
            "ROLE_SUPER_ADMIN",
            "ROLE_ADMIN",
            "ROLE_FINANCE_ADMIN",
            "ROLE_SUPPORT_ADMIN",
            "ROLE_CATALOG_ADMIN",
            "ROLE_MARKETING_ADMIN",
            "ROLE_VENDOR",
            "ROLE_INVENTORY_MANAGER",
            "ROLE_ORDER_MANAGER",
            "ROLE_VENDOR_MARKETING",
            "ROLE_VENDOR_SUPPORT",
            "ROLE_CUSTOMER",
            "ROLE_DELIVERY_PARTNER",
            "ROLE_WAREHOUSE_MANAGER",
            "ROLE_FINANCE_ACCOUNTANT"
        };
        for (String roleName : roleNames) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }
    }

    private void seedUsers() {
        // ── Load all roles ──────────────────────────────────────────────
        Role superAdminRole        = roleRepository.findByName("ROLE_SUPER_ADMIN").orElseThrow();
        Role adminRole             = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Role financeAdminRole      = roleRepository.findByName("ROLE_FINANCE_ADMIN").orElseThrow();
        Role supportAdminRole      = roleRepository.findByName("ROLE_SUPPORT_ADMIN").orElseThrow();
        Role catalogAdminRole      = roleRepository.findByName("ROLE_CATALOG_ADMIN").orElseThrow();
        Role marketingAdminRole    = roleRepository.findByName("ROLE_MARKETING_ADMIN").orElseThrow();
        Role vendorRole            = roleRepository.findByName("ROLE_VENDOR").orElseThrow();
        Role inventoryManagerRole  = roleRepository.findByName("ROLE_INVENTORY_MANAGER").orElseThrow();
        Role orderManagerRole      = roleRepository.findByName("ROLE_ORDER_MANAGER").orElseThrow();
        Role vendorMarketingRole   = roleRepository.findByName("ROLE_VENDOR_MARKETING").orElseThrow();
        Role vendorSupportRole     = roleRepository.findByName("ROLE_VENDOR_SUPPORT").orElseThrow();
        Role customerRole          = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
        Role deliveryPartnerRole   = roleRepository.findByName("ROLE_DELIVERY_PARTNER").orElseThrow();
        Role warehouseManagerRole  = roleRepository.findByName("ROLE_WAREHOUSE_MANAGER").orElseThrow();
        Role financeAccountantRole = roleRepository.findByName("ROLE_FINANCE_ACCOUNTANT").orElseThrow();

        // ── Helper lambda ──────────────────────────────────────────────
        createUserIfAbsent("admin@ecommerce.com",            "admin123",     "Master Platform Owner",     superAdminRole);
        createUserIfAbsent("staffadmin@ecommerce.com",       "admin123",     "Staff Admin",               adminRole);
        createUserIfAbsent("finance@ecommerce.com",          "finance123",   "Finance Admin",             financeAdminRole);
        createUserIfAbsent("support@ecommerce.com",          "support123",   "Support Admin",             supportAdminRole);
        createUserIfAbsent("catalog@ecommerce.com",          "catalog123",   "Catalog Admin",             catalogAdminRole);
        createUserIfAbsent("marketing@ecommerce.com",        "marketing123", "Marketing Admin",           marketingAdminRole);
        createUserIfAbsent("vendor@ecommerce.com",           "vendor123",    "OrganicWorld Partner",      vendorRole);
        createUserIfAbsent("inventory@ecommerce.com",        "vendor123",    "Inventory Manager",         inventoryManagerRole);
        createUserIfAbsent("orders@ecommerce.com",           "vendor123",    "Order Manager",             orderManagerRole);
        createUserIfAbsent("vendormarketing@ecommerce.com",  "vendor123",    "Vendor Marketing Staff",    vendorMarketingRole);
        createUserIfAbsent("vendorsupport@ecommerce.com",    "vendor123",    "Vendor Support Staff",      vendorSupportRole);
        createUserIfAbsent("customer@ecommerce.com",         "customer123",  "Test Customer",             customerRole);
        createUserIfAbsent("delivery@ecommerce.com",         "delivery123",  "Delivery Partner",          deliveryPartnerRole);
        createUserIfAbsent("warehouse@ecommerce.com",        "warehouse123", "Warehouse Manager",         warehouseManagerRole);
        createUserIfAbsent("accountant@ecommerce.com",       "finance123",   "Finance Accountant",        financeAccountantRole);

        System.out.println("====== All 15 role-based demo users seeded successfully ======");
    }

    private void createUserIfAbsent(String email, String rawPassword, String fullName, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User u = new User();
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setFullName(fullName);
            u.setEnabled(true);
            u.setRoles(Collections.singleton(role));
            userRepository.save(u);
            System.out.println("====== Created user [" + fullName + "] → " + role.getName() + " ======");
        }
    }

    private void seedProducts() {
        if (!categoryRepository.existsByName("Almonds & Cashews")) {
            Category cat = new Category();
            cat.setName("Almonds & Cashews");
            cat.setDescription("Premium quality Californian Almonds and Indian Cashews.");
            categoryRepository.save(cat);
        }
        if (!categoryRepository.existsByName("Pistachios & Walnuts")) {
            Category cat = new Category();
            cat.setName("Pistachios & Walnuts");
            cat.setDescription("Roasted salted Pistachios and crunchy Walnut Kernels.");
            categoryRepository.save(cat);
        }
        if (!categoryRepository.existsByName("Raisins & Berries")) {
            Category cat = new Category();
            cat.setName("Raisins & Berries");
            cat.setDescription("Sweet Afghan Raisins and rich antioxidant Berries.");
            categoryRepository.save(cat);
        }

        if (productRepository.count() == 0) {
            User vendor = userRepository.findByEmail("vendor@ecommerce.com").orElseThrow();
            Category almondsCashews = categoryRepository.findByName("Almonds & Cashews").orElseThrow();
            Category pistasWalnuts = categoryRepository.findByName("Pistachios & Walnuts").orElseThrow();

            Product p1 = new Product();
            p1.setName("Premium California Almonds (Badam)");
            p1.setDescription("Handpicked, large-sized crisp almonds sourced directly from California orchards. Naturally rich in protein and fiber.");
            p1.setPrice(new BigDecimal("599.00"));
            p1.setImageUrl(downloadImageLocally("https://images.unsplash.com/photo-1508061253366-f7da158b6d96?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80", "badam.jpg"));
            p1.setCategory(almondsCashews);
            p1.setVendor(vendor);
            addVariants(p1, "250g", 50, "500g", 30, "1kg", 20, "ALM-CA");

            Product p2 = new Product();
            p2.setName("Organic Roasted Cashews (Kaju)");
            p2.setDescription("Premium grade whole cashews, perfectly roasted to bring out a naturally rich, buttery flavor and crunch.");
            p2.setPrice(new BigDecimal("699.00"));
            p2.setImageUrl(downloadImageLocally("https://images.unsplash.com/photo-1509440159596-0249088772ff?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80", "kaju.jpg"));
            p2.setCategory(almondsCashews);
            p2.setVendor(vendor);
            addVariants(p2, "250g", 40, "500g", 25, "1kg", 15, "CSH-RO");

            Product p3 = new Product();
            p3.setName("Premium Salted Pistachios (Pista)");
            p3.setDescription("Deliciously salty, roasted Iranian pistachios. Easy to open shells packed with premium nutrients.");
            p3.setPrice(new BigDecimal("799.00"));
            p3.setImageUrl(downloadImageLocally("https://images.unsplash.com/photo-1629115916386-b48513523b36?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80", "pista.jpg"));
            p3.setCategory(pistasWalnuts);
            p3.setVendor(vendor);
            addVariants(p3, "250g", 35, "500g", 20, "1kg", 10, "PST-SL");

            Product p4 = new Product();
            p4.setName("Kashmiri Walnut Kernels (Akhrot)");
            p4.setDescription("Rich Kashmiri walnuts. Half-kernel pieces, high in Omega-3 and brain-boosting nutrients.");
            p4.setPrice(new BigDecimal("899.00"));
            p4.setImageUrl(downloadImageLocally("https://images.unsplash.com/photo-1585445490387-f47934b73b54?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80", "akhrot.jpg"));
            p4.setCategory(pistasWalnuts);
            p4.setVendor(vendor);
            addVariants(p4, "250g", 30, "500g", 15, "1kg", 10, "WLN-KS");

            productRepository.saveAll(List.of(p1, p2, p3, p4));
            System.out.println("====== Dryfruit products and pack size variants seeded successfully ======");
        }
    }

    private void addVariants(Product product, String size1, int stock1, String size2, int stock2, String size3, int stock3, String prefix) {
        com.ecommerce.entity.ProductVariant v1 = new com.ecommerce.entity.ProductVariant();
        v1.setProduct(product);
        v1.setSize(size1);
        v1.setSku(prefix + "-" + size1.toUpperCase());
        v1.setStock(stock1);
        v1.setPrice(product.getPrice().multiply(new java.math.BigDecimal("0.5")));
        v1.setUnitPrice(v1.getPrice().multiply(new java.math.BigDecimal("0.70")));

        com.ecommerce.entity.ProductVariant v2 = new com.ecommerce.entity.ProductVariant();
        v2.setProduct(product);
        v2.setSize(size2);
        v2.setSku(prefix + "-" + size2.toUpperCase());
        v2.setStock(stock2);
        v2.setPrice(product.getPrice());
        v2.setUnitPrice(v2.getPrice().multiply(new java.math.BigDecimal("0.70")));

        com.ecommerce.entity.ProductVariant v3 = new com.ecommerce.entity.ProductVariant();
        v3.setProduct(product);
        v3.setSize(size3);
        v3.setSku(prefix + "-" + size3.toUpperCase());
        v3.setStock(stock3);
        v3.setPrice(product.getPrice().multiply(new java.math.BigDecimal("1.8")));
        v3.setUnitPrice(v3.getPrice().multiply(new java.math.BigDecimal("0.70")));

        product.getVariants().add(v1);
        product.getVariants().add(v2);
        product.getVariants().add(v3);
    }

    private void seedOrdersAndPayments() {
        if (orderRepository.count() == 0) {
            User customer = userRepository.findByEmail("customer@ecommerce.com").orElseThrow();
            User vendor = userRepository.findByEmail("vendor@ecommerce.com").orElseThrow();
            List<Product> products = productRepository.findAllWithVariants();
            if (products.isEmpty()) return;

            // Order 1: Delivered in March 2026
            com.ecommerce.entity.Order o1 = new com.ecommerce.entity.Order();
            o1.setUser(customer);
            o1.setTotalAmount(new BigDecimal("1198.00"));
            o1.setStatus(com.ecommerce.entity.OrderStatus.DELIVERED);
            o1.setCreatedAt(java.time.LocalDateTime.of(2026, 3, 15, 14, 30));
            
            com.ecommerce.entity.OrderItem oi1 = new com.ecommerce.entity.OrderItem();
            oi1.setOrder(o1);
            oi1.setProduct(products.get(0));
            if (!products.get(0).getVariants().isEmpty()) {
                oi1.setVariant(products.get(0).getVariants().get(0));
            }
            oi1.setQuantity(2);
            oi1.setPrice(new BigDecimal("599.00"));
            oi1.setVendor(vendor);
            o1.getItems().add(oi1);
            orderRepository.save(o1);

            com.ecommerce.entity.Payment pay1 = new com.ecommerce.entity.Payment();
            pay1.setOrder(o1);
            pay1.setAmount(new BigDecimal("1198.00"));
            pay1.setRazorpayOrderId("rzp_ord_march");
            pay1.setRazorpayPaymentId("pay_march_success");
            pay1.setStatus("SUCCESS");
            paymentRepository.save(pay1);

            // Order 2: Paid in April 2026
            com.ecommerce.entity.Order o2 = new com.ecommerce.entity.Order();
            o2.setUser(customer);
            o2.setTotalAmount(new BigDecimal("699.00"));
            o2.setStatus(com.ecommerce.entity.OrderStatus.PAID);
            o2.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 10, 11, 15));

            com.ecommerce.entity.OrderItem oi2 = new com.ecommerce.entity.OrderItem();
            oi2.setOrder(o2);
            oi2.setProduct(products.get(1));
            if (!products.get(1).getVariants().isEmpty()) {
                oi2.setVariant(products.get(1).getVariants().get(0));
            }
            oi2.setQuantity(1);
            oi2.setPrice(new BigDecimal("699.00"));
            oi2.setVendor(vendor);
            o2.getItems().add(oi2);
            orderRepository.save(o2);

            com.ecommerce.entity.Payment pay2 = new com.ecommerce.entity.Payment();
            pay2.setOrder(o2);
            pay2.setAmount(new BigDecimal("699.00"));
            pay2.setRazorpayOrderId("rzp_ord_april");
            pay2.setRazorpayPaymentId("pay_april_success");
            pay2.setStatus("SUCCESS");
            paymentRepository.save(pay2);

            // Order 3: Disputed in May 2026
            com.ecommerce.entity.Order o3 = new com.ecommerce.entity.Order();
            o3.setUser(customer);
            o3.setTotalAmount(new BigDecimal("1438.20"));
            o3.setStatus(com.ecommerce.entity.OrderStatus.PENDING);
            o3.setCreatedAt(java.time.LocalDateTime.of(2026, 5, 2, 16, 45));

            com.ecommerce.entity.OrderItem oi3 = new com.ecommerce.entity.OrderItem();
            oi3.setOrder(o3);
            oi3.setProduct(products.get(3));
            if (!products.get(3).getVariants().isEmpty()) {
                oi3.setVariant(products.get(3).getVariants().get(0));
            }
            oi3.setQuantity(1);
            oi3.setPrice(new BigDecimal("1438.20"));
            oi3.setVendor(vendor);
            o3.getItems().add(oi3);
            orderRepository.save(o3);

            com.ecommerce.entity.Payment pay3 = new com.ecommerce.entity.Payment();
            pay3.setOrder(o3);
            pay3.setAmount(new BigDecimal("1438.20"));
            pay3.setRazorpayOrderId("rzp_ord_may_disputed");
            pay3.setRazorpayPaymentId("pay_customer_debited_777");
            pay3.setStatus("DISPUTED");
            paymentRepository.save(pay3);

            // Seed Chronological Audit Trail Records for full visibility
            try {
                jdbcTemplate.execute("INSERT INTO payment_audit_logs (payment_id, previous_state, new_state, action_by, details, timestamp) VALUES (1, 'CREATED', 'SUCCESS', 'SYSTEM', 'Payment completed successfully via mock gateway', '2026-03-15 14:35:00')");
                jdbcTemplate.execute("INSERT INTO payment_audit_logs (payment_id, previous_state, new_state, action_by, details, timestamp) VALUES (2, 'CREATED', 'SUCCESS', 'SYSTEM', 'Payment completed successfully via mock gateway', '2026-04-10 11:20:00')");
                jdbcTemplate.execute("INSERT INTO payment_audit_logs (payment_id, previous_state, new_state, action_by, details, timestamp) VALUES (3, 'CREATED', 'PENDING', 'CUSTOMER', 'Customer initiated checkout session', '2026-05-02 16:45:00')");
                jdbcTemplate.execute("INSERT INTO payment_audit_logs (payment_id, previous_state, new_state, action_by, details, timestamp) VALUES (3, 'PENDING', 'DISPUTED', 'CUSTOMER', 'Customer submitted dispute report: Payment debited but order pending', '2026-05-02 17:10:00')");
            } catch (Exception ex) {
                System.out.println("====== Skipping Audit Logs seed: " + ex.getMessage() + " ======");
            }

            System.out.println("====== Seeded historical mock orders and payments for Reports & Analytics ======");
        }
    }

    private String downloadImageLocally(String urlString, String filename) {
        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }
            java.nio.file.Path localFilePath = uploadPath.resolve(filename);

            if (!java.nio.file.Files.exists(localFilePath)) {
                java.net.URL url = new java.net.URI(urlString).toURL();
                try (java.io.InputStream in = url.openStream()) {
                    java.nio.file.Files.copy(in, localFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return "/uploads/" + filename;
        } catch (Exception e) {
            System.out.println("Warning: Failed to download local image from " + urlString + " due to " + e.getMessage() + ". Using fallback path.");
            return "/uploads/" + filename;
        }
    }
}
