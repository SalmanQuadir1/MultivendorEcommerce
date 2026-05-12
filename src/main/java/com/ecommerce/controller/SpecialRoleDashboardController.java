package com.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpecialRoleDashboardController {

    // ── Delivery Partner ────────────────────────────────────────────────
    @GetMapping("/delivery/dashboard")
    public String deliveryDashboard() {
        return "delivery/dashboard";
    }

    // ── Warehouse Manager ───────────────────────────────────────────────
    @GetMapping("/warehouse/dashboard")
    public String warehouseDashboard() {
        return "warehouse/dashboard";
    }

    // ── Finance Accountant ──────────────────────────────────────────────
    @GetMapping("/finance/dashboard")
    public String financeDashboard() {
        return "finance/dashboard";
    }
}
