package com.ecommerce.controller;

import com.ecommerce.entity.Payment;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final com.ecommerce.repository.OrderRepository orderRepository;
    private final com.ecommerce.service.RazorpayService razorpayService;

    @GetMapping("/admin/payments")
    public String listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        
        String cleanStatus = (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) ? null : status.trim();
        String cleanSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();

        Page<Payment> paymentsPage = paymentRepository.searchPayments(cleanStatus, cleanSearch, pageable);
        
        // Fetch all payments to calculate dynamic metrics
        java.util.List<Payment> allPayments = paymentRepository.findAll();
        
        java.math.BigDecimal totalVolume = allPayments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
        java.math.BigDecimal pendingVolume = allPayments.stream()
                .filter(p -> "CREATED".equals(p.getStatus()) || "PENDING".equals(p.getStatus()) || "DISPUTED".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long successCount = allPayments.stream().filter(p -> "SUCCESS".equals(p.getStatus())).count();
        long totalCount = allPayments.size();
        double successRate = totalCount > 0 ? ((double) successCount / totalCount) * 100 : 100.0;
        long failedCount = allPayments.stream().filter(p -> "FAILED".equals(p.getStatus())).count();

        model.addAttribute("payments", paymentsPage);
        model.addAttribute("totalVolume", totalVolume);
        model.addAttribute("pendingVolume", pendingVolume);
        model.addAttribute("successRate", String.format("%.1f", successRate));
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("status", status != null ? status : "ALL");
        
        return "admin/payments/list";
    }

    @PostMapping("/admin/payments/toggle/{id}")
    public String togglePaymentStatus(@PathVariable("id") Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment != null) {
            if ("CREATED".equals(payment.getStatus()) || "PENDING".equals(payment.getStatus()) || "DISPUTED".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
                payment.setStatus("SUCCESS");
                if (payment.getOrder() != null) {
                    payment.getOrder().setStatus(com.ecommerce.entity.OrderStatus.PAID);
                }
            } else if ("SUCCESS".equals(payment.getStatus())) {
                payment.setStatus("CREATED");
                if (payment.getOrder() != null) {
                    payment.getOrder().setStatus(com.ecommerce.entity.OrderStatus.PENDING);
                }
            }
            paymentRepository.save(payment);
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/admin/payments/update")
    public String updatePaymentDetails(
            @RequestParam("paymentId") Long paymentId,
            @RequestParam(value = "razorpayPaymentId", required = false) String razorpayPaymentId,
            @RequestParam("status") String status,
            @RequestParam("amount") java.math.BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment != null) {
            payment.setRazorpayPaymentId(razorpayPaymentId != null && !razorpayPaymentId.trim().isEmpty() ? razorpayPaymentId : null);
            payment.setStatus(status);
            payment.setAmount(amount);
            if (payment.getOrder() != null) {
                com.ecommerce.entity.Order order = payment.getOrder();
                if ("SUCCESS".equals(status)) {
                    order.setStatus(com.ecommerce.entity.OrderStatus.PAID);
                } else {
                    order.setStatus(com.ecommerce.entity.OrderStatus.PENDING);
                }
                orderRepository.save(order);
            }
            paymentRepository.save(payment);
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/admin/payments/refund")
    public String refundPayment(
            @RequestParam("paymentId") Long paymentId,
            @RequestParam("amount") java.math.BigDecimal amount,
            @RequestParam(value = "reason", required = false) String reason) {
        try {
            razorpayService.processRefund(paymentId, amount, reason != null && !reason.trim().isEmpty() ? reason : "Customer Request");
        } catch (Exception e) {
            System.err.println("Refund processing error: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/admin/payments/capture/{id}")
    public String capturePayment(@PathVariable("id") Long paymentId) {
        try {
            razorpayService.manuallyCapturePayment(paymentId);
        } catch (Exception e) {
            System.err.println("Capture processing error: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/admin/payments/resolve-dispute")
    public String resolveDispute(
            @RequestParam("paymentId") Long paymentId,
            @RequestParam("action") String action,
            @RequestParam(value = "notes", required = false) String notes) {
        try {
            razorpayService.resolveDispute(paymentId, action, notes != null && !notes.trim().isEmpty() ? notes : "Resolved by Administrator");
        } catch (Exception e) {
            System.err.println("Dispute resolution error: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @GetMapping("/admin/payments/audit-logs/{id}")
    @ResponseBody
    public java.util.List<com.ecommerce.entity.PaymentAuditLog> getAuditLogs(@PathVariable("id") Long paymentId) {
        return razorpayService.getAuditLogsForPayment(paymentId);
    }
    
    @PostMapping("/admin/payments/sync/{id}")
    public String syncPaymentStatus(@PathVariable("id") Long paymentId) {
        try {
            razorpayService.syncPaymentStatus(paymentId);
        } catch (Exception e) {
            System.err.println("Sync processing error: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}
