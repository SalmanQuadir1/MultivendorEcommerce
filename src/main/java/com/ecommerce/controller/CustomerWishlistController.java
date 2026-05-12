package com.ecommerce.controller;

import com.ecommerce.entity.WishlistItem;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.WishlistItemRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/customer/wishlist")
@RequiredArgsConstructor
public class CustomerWishlistController {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String viewWishlist(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        List<WishlistItem> wishlist = wishlistItemRepository.findByUserIdOrderByAddedAtDesc(userDetails.getId());
        model.addAttribute("wishlist", wishlist);
        return "customer/wishlist";
    }

    @PostMapping("/add/{productId}")
    public String addToWishlist(@PathVariable("productId") Long productId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestHeader(value = "Referer", required = false) String referer) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        boolean exists = wishlistItemRepository.existsByUserIdAndProductId(userDetails.getId(), productId);
        if (!exists) {
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user != null && product != null) {
                WishlistItem item = new WishlistItem();
                item.setUser(user);
                item.setProduct(product);
                item.setAddedAt(LocalDateTime.now());
                wishlistItemRepository.save(item);
            }
        }
        
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/customer/wishlist";
    }

    @PostMapping("/remove/{productId}")
    public String removeFromWishlist(@PathVariable("productId") Long productId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestHeader(value = "Referer", required = false) String referer) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        wishlistItemRepository.deleteByUserIdAndProductId(userDetails.getId(), productId);
        
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/customer/wishlist";
    }

    @PostMapping("/toggle/{productId}")
    @ResponseBody
    public java.util.Map<String, Object> toggleWishlist(@PathVariable("productId") Long productId,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return response;
        }

        boolean exists = wishlistItemRepository.existsByUserIdAndProductId(userDetails.getId(), productId);
        if (exists) {
            wishlistItemRepository.deleteByUserIdAndProductId(userDetails.getId(), productId);
            response.put("success", true);
            response.put("added", false);
            response.put("message", "Removed from wishlist");
        } else {
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user != null && product != null) {
                WishlistItem item = new WishlistItem();
                item.setUser(user);
                item.setProduct(product);
                item.setAddedAt(LocalDateTime.now());
                wishlistItemRepository.save(item);
                response.put("success", true);
                response.put("added", true);
                response.put("message", "Added to wishlist");
            } else {
                response.put("success", false);
                response.put("message", "Product or user not found");
            }
        }
        return response;
    }
}
