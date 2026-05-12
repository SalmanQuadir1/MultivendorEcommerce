package com.ecommerce.service;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.razorpay.RazorpayException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RazorpayService {
    Payment createRazorpayOrder(Order order) throws RazorpayException;
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature) throws RazorpayException;
    
    // Refunds (Full & Partial)
    Payment processRefund(Long paymentId, BigDecimal amount, String reason) throws RazorpayException;
    
    // Status Querying & History
    Payment queryPaymentStatus(String razorpayPaymentId) throws RazorpayException;
    List<Payment> getTransactionHistory(Long userId);
    
    // Handle 3D Secure / SCA
    Map<String, Object> handle3DSecureAuthentication(String paymentId) throws RazorpayException;
    
    // Secure Webhook Handler
    boolean processWebhook(String payload, String signatureHeader) throws Exception;

    // Advanced Control Functions
    Payment manuallyCapturePayment(Long paymentId) throws RazorpayException;
    Payment resolveDispute(Long paymentId, String action, String notes) throws RazorpayException;
    List<com.ecommerce.entity.PaymentAuditLog> getAuditLogsForPayment(Long paymentId);
    void logTransactionAudit(Long paymentId, String previousState, String newState, String actionBy, String details);
    Payment syncPaymentStatus(Long paymentId) throws RazorpayException;
}
