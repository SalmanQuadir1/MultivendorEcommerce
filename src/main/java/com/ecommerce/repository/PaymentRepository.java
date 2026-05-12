package com.ecommerce.repository;
import com.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "CAST(p.id AS string) LIKE %:search% OR " +
           "CAST(p.order.id AS string) LIKE %:search% OR " +
           "LOWER(p.order.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.order.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<Payment> searchPayments(@org.springframework.data.repository.query.Param("status") String status, 
                                 @org.springframework.data.repository.query.Param("search") String search, 
                                 org.springframework.data.domain.Pageable pageable);
}
