package com.ecommerce.service.impl;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("cartService")
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    public Cart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            User user = userRepository.findById(userId).orElseThrow();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Override
    public void addToCart(Long userId, Long variantId, int quantity) {
        Cart cart = getCartForUser(userId);
        com.ecommerce.entity.ProductVariant variant = productVariantRepository.findById(variantId).orElseThrow();

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant() != null && item.getVariant().getId().equals(variantId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(variant.getProduct());
            item.setVariant(variant);
            item.setQuantity(quantity);
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }
    }

    @Override
    public void removeFromCart(Long userId, Long cartItemId) {
        Cart cart = getCartForUser(userId);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getCartForUser(userId);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
    }

    @Override
    public Cart getCartForCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String email = auth.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return getCartForUser(userOpt.get().getId());
            }
        }
        return null;
    }
}
