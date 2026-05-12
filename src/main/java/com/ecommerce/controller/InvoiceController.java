package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.PurchaseOrder;
import com.ecommerce.entity.PurchaseOrderItem;
import com.ecommerce.repository.PurchaseOrderRepository;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final OrderService orderService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @GetMapping("/sale/{orderId}")
    public String saleInvoice(@PathVariable Long orderId, Model model) {
        Order order = orderService.findById(orderId);
        if (order == null) return "redirect:/";

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            subtotal = subtotal.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        model.addAttribute("order", order);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("date", order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        model.addAttribute("invNo", "INV-S-" + String.format("%06d", order.getId()));
        return "invoice/sale";
    }

    @GetMapping("/purchase/{poId}")
    public String purchaseInvoice(@PathVariable Long poId, Model model) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId).orElse(null);
        if (po == null) return "redirect:/";

        model.addAttribute("po", po);
        model.addAttribute("date", po.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        model.addAttribute("invNo", "INV-P-" + String.format("%06d", po.getId()));
        return "invoice/purchase";
    }
}
