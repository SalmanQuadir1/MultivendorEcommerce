package com.ecommerce.controller;

import com.ecommerce.dto.UserRegistrationDto;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final com.ecommerce.service.ProductService productService;
    private final com.ecommerce.repository.CarouselItemRepository carouselItemRepository;
    private final com.ecommerce.repository.WishlistItemRepository wishlistItemRepository;
    private final com.ecommerce.repository.CategoryRepository categoryRepository;
    private final com.ecommerce.repository.CouponRepository couponRepository;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(@Valid @ModelAttribute("user") UserRegistrationDto registrationDto,
                                      BindingResult result,
                                      Model model) {
        User existingUser = userService.findByEmail(registrationDto.getEmail());
        if (existingUser != null) {
            result.rejectValue("email", null, "There is already an account registered with that email");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", registrationDto);
            return "register";
        }

        userService.save(registrationDto);
        return "redirect:/register?success";
    }
    
    @GetMapping("/")
    public String home(@org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.security.CustomUserDetails userDetails, Model model) {
        
        // Featured products (for any generic use)
        model.addAttribute("products", productService.findAll(
            org.springframework.data.domain.PageRequest.of(0, 8)).getContent());

        // Categories for category grid
        model.addAttribute("categories", categoryRepository.findAll());

        // New Arrivals — newest first
        java.util.List<com.ecommerce.entity.Product> newArrivals = productService.findAll(
            org.springframework.data.domain.PageRequest.of(0, 12,
                org.springframework.data.domain.Sort.by("id").descending())).getContent();
        model.addAttribute("newArrivals", newArrivals);

        // Best Sellers — sort by price desc as visual differentiation; fallback to newArrivals if page 1 empty
        java.util.List<com.ecommerce.entity.Product> bestSellers = productService.findAll(
            org.springframework.data.domain.PageRequest.of(0, 12,
                org.springframework.data.domain.Sort.by("price").descending())).getContent();
        model.addAttribute("bestSellers", bestSellers);

        // Wishlisted product IDs for heart icons
        java.util.List<Long> wishlistedIds = new java.util.ArrayList<>();
        if (userDetails != null) {
            wishlistedIds = wishlistItemRepository.findByUserIdOrderByAddedAtDesc(userDetails.getId())
                    .stream()
                    .map(item -> item.getProduct().getId())
                    .collect(java.util.stream.Collectors.toList());
        }
        model.addAttribute("wishlistedIds", wishlistedIds);

        // Carousel slides
        java.util.List<com.ecommerce.entity.CarouselItem> slides = carouselItemRepository.findByActiveTrue();
        if (slides.isEmpty()) {
            carouselItemRepository.deleteAll();
            com.ecommerce.entity.CarouselItem s1 = com.ecommerce.entity.CarouselItem.builder()
                    .imageUrl("https://images.unsplash.com/photo-1626351801418-22200373ef12?auto=format&fit=crop&w=1920&q=80")
                    .title("PREMIUM ORGANIC").subtitle("Experience the richest, natural-sun-dried flavor direct from elite orchards.")
                    .actionUrl("/products").active(true).build();
            com.ecommerce.entity.CarouselItem s2 = com.ecommerce.entity.CarouselItem.builder()
                    .imageUrl("https://images.unsplash.com/photo-1609167830220-7164aa360951?auto=format&fit=crop&w=1920&q=80")
                    .title("SUPERFOOD NUTS").subtitle("Loaded with clean proteins, essential omegas, and crystalline energy.")
                    .actionUrl("/products").active(true).build();
            com.ecommerce.entity.CarouselItem s3 = com.ecommerce.entity.CarouselItem.builder()
                    .imageUrl("https://images.unsplash.com/photo-1573274787165-c1834439c112?auto=format&fit=crop&w=1920&q=80")
                    .title("GOURMET INDULGENCE").subtitle("Handpicked collection of non-GMO wholesome goodness you can trust.")
                    .actionUrl("/products").active(true).build();
            carouselItemRepository.save(s1);
            carouselItemRepository.save(s2);
            carouselItemRepository.save(s3);
            slides = carouselItemRepository.findByActiveTrue();
        }
        model.addAttribute("carouselItems", slides);

        // Active coupons
        model.addAttribute("activeCoupons", couponRepository.findActiveValidCoupons(java.time.LocalDate.now()));

        return "index";
    }

    @GetMapping("/test")
    public String test() {
        return "Test page works!";
    }
}
