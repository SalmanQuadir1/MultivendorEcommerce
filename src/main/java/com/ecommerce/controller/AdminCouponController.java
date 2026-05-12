package com.ecommerce.controller;

import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.CouponRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String listCoupons(Model model) {
        model.addAttribute("coupons", couponRepository.findAll());
        model.addAttribute("newCoupon", new Coupon());
        model.addAttribute("allProducts", productRepository.findAll());
        return "admin/coupons/list";
    }

    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute("newCoupon") Coupon coupon,
                             @RequestParam(value = "selectedProductIds", required = false) java.util.List<Long> selectedProductIds) {
        coupon.setUsedCount(0);
        if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
            coupon.setApplicableProducts(selectedProductIds.stream()
                .map(id -> productRepository.findById(id).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList()));
        } else {
            coupon.setApplicableProducts(new ArrayList<>());
        }
        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @PostMapping("/toggle/{id}")
    public String toggleCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id).orElse(null);
        if (coupon != null) {
            coupon.setActive(!coupon.isActive());
            couponRepository.save(coupon);
        }
        return "redirect:/admin/coupons";
    }

    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id) {
        couponRepository.deleteById(id);
        return "redirect:/admin/coupons";
    }
}
