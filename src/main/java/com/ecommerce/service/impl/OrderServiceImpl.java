package com.ecommerce.service.impl;

import com.ecommerce.entity.*;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartService cartService;
    private final com.ecommerce.repository.InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    public Order createOrderFromCart(Long userId) {
        Cart cart = cartService.getCartForUser(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        java.util.List<com.ecommerce.entity.InventoryTransaction> transactionsToSave = new java.util.ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getVariant() != null ? cartItem.getVariant().getPrice() : cartItem.getProduct().getPrice());
            orderItem.setVendor(cartItem.getProduct().getVendor());

            ProductVariant variant = cartItem.getVariant();
            if (variant != null) {
                if (variant.getStock() < cartItem.getQuantity()) {
                    throw new RuntimeException("Not enough stock for " + cartItem.getProduct().getName() + " - Size: " + variant.getSize());
                }
                variant.setStock(variant.getStock() - cartItem.getQuantity());
                productVariantRepository.save(variant);

                com.ecommerce.entity.InventoryTransaction tx = new com.ecommerce.entity.InventoryTransaction();
                tx.setVariant(variant);
                tx.setTransactionType(com.ecommerce.entity.InventoryTransactionType.SALE);
                tx.setQuantityChange(-cartItem.getQuantity());
                transactionsToSave.add(tx);
            }

            order.getItems().add(orderItem);

            total = total.add(orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity())));
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        for (com.ecommerce.entity.InventoryTransaction tx : transactionsToSave) {
            tx.setReference("Sale Order #" + savedOrder.getId());
            inventoryTransactionRepository.save(tx);
        }
        
        cartService.clearCart(userId);
        
        return savedOrder;
    }

    @Override
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    public List<Order> findUserOrders(Long userId) {
        return orderRepository.findByUserIdWithItems(userId);
    }

    @Override
    public Page<Order> findUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdWithItems(userId, pageable);
    }

    @Override
    public List<OrderItem> findVendorOrderItems(Long vendorId) {
        return orderItemRepository.findByVendorId(vendorId);
    }

    @Override
    public Page<OrderItem> findVendorOrderItems(Long vendorId, Pageable pageable) {
        return orderItemRepository.findByVendorId(vendorId, pageable);
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        Order order = findById(orderId);
        if (order != null) {
            order.setStatus(OrderStatus.valueOf(status));
            orderRepository.save(order);
        }
    }
}
