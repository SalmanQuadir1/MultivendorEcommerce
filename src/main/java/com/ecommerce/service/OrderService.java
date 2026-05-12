package com.ecommerce.service;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    Order createOrderFromCart(Long userId);
    Order findById(Long id);
    Order findByIdSimple(Long id);
    List<Order> findUserOrders(Long userId);
    Page<Order> findUserOrders(Long userId, Pageable pageable);
    List<OrderItem> findVendorOrderItems(Long vendorId);
    Page<OrderItem> findVendorOrderItems(Long vendorId, Pageable pageable);
    void updateOrderStatus(Long orderId, String status);
    Order applyCouponDiscount(Long orderId, Coupon coupon, BigDecimal discountAmount);
    Order save(Order order);
    void clearCartAfterPayment(Long orderId);
}
