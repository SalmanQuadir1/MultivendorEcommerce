package com.ecommerce.repository;

import com.ecommerce.entity.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {
    List<PaymentAuditLog> findByPaymentIdOrderByTimestampDesc(Long paymentId);
}
