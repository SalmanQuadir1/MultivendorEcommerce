package com.ecommerce.service;
import com.ecommerce.entity.Cart;

public interface CartService {
    Cart getCartForUser(Long userId);
    void addToCart(Long userId, Long variantId, int quantity);
    void removeFromCart(Long userId, Long cartItemId);
    void clearCart(Long userId);
    Cart getCartForCurrentUser();
}
