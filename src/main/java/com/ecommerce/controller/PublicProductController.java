package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final com.ecommerce.repository.WishlistItemRepository wishlistItemRepository;
    private final com.ecommerce.service.ReviewService reviewService;
    private final com.ecommerce.repository.VendorPincodeRepository vendorPincodeRepository;

    @GetMapping("/api/products/{id}/check-availability")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> checkAvailability(
            @PathVariable("id") Long productId,
            @RequestParam("pincode") String pincode) {
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        Product product = productService.findById(productId);
        
        if (product.getVendor() == null) {
            response.put("available", true);
            response.put("message", "Delivery available! Expected arrival in 3-5 business days.");
            return response;
        }

        // Determine if vendor has restricted delivery areas
        java.util.List<com.ecommerce.entity.VendorPincode> pins = vendorPincodeRepository.findByVendorId(product.getVendor().getId());
        
        if (pins == null || pins.isEmpty()) {
            // If vendor hasn't limited coverage yet, default to nationwide
            response.put("available", true);
            response.put("message", "Super-fast Delivery available to this location!");
        } else {
            // Check exact match
            boolean match = pins.stream().anyMatch(p -> p.getPincode().equalsIgnoreCase(pincode.trim()));
            response.put("available", match);
            if (match) {
                response.put("message", "Great news! Delivery available for this pincode.");
            } else {
                response.put("message", "Sorry, this product is currently not serviceable at your location.");
            }
        }
        return response;
    }

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        // Build sort
        Sort sortOrder;
        switch (sort) {
            case "price_asc"  -> sortOrder = Sort.by("price").ascending();
            case "price_desc" -> sortOrder = Sort.by("price").descending();
            case "name_asc"   -> sortOrder = Sort.by("name").ascending();
            default           -> sortOrder = Sort.by("id").descending(); // newest
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Normalize keyword — treat blank as null so query falls through
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        org.springframework.data.domain.Page<Product> productsPage =
                productRepository.searchProducts(kw, categoryId, minPrice, maxPrice, pageable);

        model.addAttribute("products", productsPage);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", kw);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("totalResults", productsPage.getTotalElements());

        java.util.List<Long> wishlistedIds = new java.util.ArrayList<>();
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.ecommerce.security.CustomUserDetails) {
            com.ecommerce.security.CustomUserDetails userDetails =
                    (com.ecommerce.security.CustomUserDetails) auth.getPrincipal();
            wishlistedIds = wishlistItemRepository.findByUserIdOrderByAddedAtDesc(userDetails.getId())
                    .stream()
                    .map(item -> item.getProduct().getId())
                    .collect(java.util.stream.Collectors.toList());
        }
        model.addAttribute("wishlistedIds", wishlistedIds);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable("id") Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        
        boolean isWishlisted = false;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.ecommerce.security.CustomUserDetails) {
            com.ecommerce.security.CustomUserDetails userDetails = (com.ecommerce.security.CustomUserDetails) auth.getPrincipal();
            isWishlisted = wishlistItemRepository.existsByUserIdAndProductId(userDetails.getId(), id);
        }
        
        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("reviews", reviewService.getReviewsByProduct(id));
        model.addAttribute("averageRating", reviewService.getAverageRating(id));
        return "products/details";
    }
}
