package com.ecommerce.controller;

import com.ecommerce.entity.OrderItem;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/vendor/orders")
@RequiredArgsConstructor
public class VendorOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<OrderItem> orderItems = orderService.findVendorOrderItems(userDetails.getId());
        model.addAttribute("orderItems", orderItems);
        return "vendor/orders/list";
    }
}
