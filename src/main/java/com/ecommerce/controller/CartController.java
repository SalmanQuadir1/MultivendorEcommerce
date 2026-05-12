package com.ecommerce.controller;

import com.ecommerce.entity.Cart;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    private final com.ecommerce.repository.ProductRepository productRepository;

    @GetMapping
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cart cart = cartService.getCartForUser(userDetails.getId());
        model.addAttribute("cart", cart);
        return "customer/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam(value = "variantId", required = false) Long variantId,
                            @RequestParam(value = "productId", required = false) Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (variantId != null) {
            cartService.addToCart(userDetails.getId(), variantId, quantity);
        } else if (productId != null) {
            com.ecommerce.entity.Product product = productRepository.findById(productId).orElseThrow();
            if (!product.getVariants().isEmpty()) {
                Long defaultVariantId = product.getVariants().get(0).getId();
                cartService.addToCart(userDetails.getId(), defaultVariantId, quantity);
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable("itemId") Long itemId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.removeFromCart(userDetails.getId(), itemId);
        return "redirect:/cart";
    }
}
