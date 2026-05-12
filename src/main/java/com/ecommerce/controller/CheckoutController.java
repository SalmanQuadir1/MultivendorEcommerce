package com.ecommerce.controller;

import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.repository.CouponRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.RazorpayService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final com.ecommerce.repository.AddressRepository addressRepository;
    private final com.ecommerce.repository.UserRepository userRepository;
    private final com.ecommerce.repository.PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @GetMapping
    public String showCheckout(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            Long orderId = (Long) session.getAttribute("currentOrderId");
            if (orderId == null) {
                return "redirect:/cart";
            }
            Order order = orderService.findById(orderId);
            if (order == null) {
                return "redirect:/cart";
            }
            return renderCheckout(order, userDetails, model);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading checkout: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping
    public String processCheckout(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            session.removeAttribute("appliedCouponCode");
            session.removeAttribute("discountAmount");
            Order order = orderService.createOrderFromCart(userDetails.getId());
            session.setAttribute("currentOrderId", order.getId());
            return renderCheckout(order, userDetails, model);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing checkout: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    private String renderCheckout(Order order, CustomUserDetails userDetails, Model model) throws Exception {
        Payment payment = razorpayService.createRazorpayOrder(order);
        java.util.List<com.ecommerce.entity.Address> addresses = addressRepository.findByUserId(userDetails.getId());
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("razorpayKeyId", keyId);
        model.addAttribute("userFullName", userDetails.getFullName());
        model.addAttribute("userEmail", userDetails.getUsername());
        model.addAttribute("addresses", addresses);
        return "customer/checkout";
    }

    private BigDecimal calculateApplicableDiscount(Coupon coupon, Order order) {
        java.util.List<Long> applicableIds = coupon.getApplicableProducts().stream()
            .map(com.ecommerce.entity.Product::getId).collect(java.util.stream.Collectors.toList());
        BigDecimal applicableTotal = BigDecimal.ZERO;
        for (com.ecommerce.entity.OrderItem item : order.getItems()) {
            if (applicableIds.isEmpty() || applicableIds.contains(item.getProduct().getId())) {
                applicableTotal = applicableTotal.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        return coupon.calculateDiscount(applicableTotal);
    }

    @PostMapping("/apply-coupon")
    public String applyCoupon(@RequestParam("couponCode") String couponCode,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase()).orElse(null);
            if (coupon == null) {
                redirectAttributes.addFlashAttribute("couponError", "Invalid coupon code.");
                return "redirect:/checkout";
            }
            if (!coupon.isValid()) {
                redirectAttributes.addFlashAttribute("couponError", "Coupon has expired or reached its usage limit.");
                return "redirect:/checkout";
            }
            Long orderId = (Long) session.getAttribute("currentOrderId");
            if (orderId == null) {
                redirectAttributes.addFlashAttribute("couponError", "No active order found.");
                return "redirect:/cart";
            }
            Order order = orderService.findById(orderId);
            if (order == null) {
                redirectAttributes.addFlashAttribute("couponError", "No active order found.");
                return "redirect:/cart";
            }
            if (order.getCouponCode() != null) {
                redirectAttributes.addFlashAttribute("couponError", "Coupon already applied. Remove it first.");
                return "redirect:/checkout";
            }
            BigDecimal discount = calculateApplicableDiscount(coupon, order);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("couponError", "Coupon not applicable. Check minimum order requirements.");
                return "redirect:/checkout";
            }
            orderService.applyCouponDiscount(order.getId(), coupon, discount);
            session.setAttribute("appliedCouponCode", coupon.getCode());
            session.setAttribute("discountAmount", discount);
            redirectAttributes.addFlashAttribute("couponSuccess", "Coupon " + coupon.getCode() + " applied! You saved ₹" + discount);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("couponError", "Error: " + e.getMessage());
        }
        return "redirect:/checkout";
    }

    @PostMapping("/remove-coupon")
    public String removeCoupon(HttpSession session, RedirectAttributes redirectAttributes) {
        Long orderId = (Long) session.getAttribute("currentOrderId");
        if (orderId != null) {
            Order order = orderService.findByIdSimple(orderId);
            if (order != null && order.getDiscountAmount() != null) {
                order.setTotalAmount(order.getTotalAmount().add(order.getDiscountAmount()));
                order.setCouponCode(null);
                order.setDiscountAmount(null);
                orderService.save(order);
            }
        }
        session.removeAttribute("appliedCouponCode");
        session.removeAttribute("discountAmount");
        return "redirect:/checkout";
    }

    @PostMapping("/address/add")
    @ResponseBody
    public java.util.Map<String, Object> addAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam("streetAddress") String streetAddress,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("pinCode") String pinCode,
            @RequestParam("contactNumber") String contactNumber) {
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            com.ecommerce.entity.User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String finalFullName = (fullName == null || fullName.trim().isEmpty()) ? user.getFullName() : fullName;
            
            com.ecommerce.entity.Address address = com.ecommerce.entity.Address.builder()
                    .user(user)
                    .fullName(finalFullName)
                    .streetAddress(streetAddress)
                    .city(city)
                    .state(state)
                    .pinCode(pinCode)
                    .contactNumber(contactNumber)
                    .isDefault(true)
                    .build();
                    
            addressRepository.save(address);
            
            java.util.Map<String, Object> addrMap = new java.util.HashMap<>();
            addrMap.put("id", address.getId());
            addrMap.put("fullName", address.getFullName());
            addrMap.put("streetAddress", address.getStreetAddress());
            addrMap.put("city", address.getCity());
            addrMap.put("state", address.getState());
            addrMap.put("pinCode", address.getPinCode());
            addrMap.put("contactNumber", address.getContactNumber());

            response.put("success", true);
            response.put("address", addrMap);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/dispute")
    @ResponseBody
    public java.util.Map<String, Object> reportDispute(
            @RequestParam("orderId") Long orderId,
            @RequestParam("razorpayPaymentId") String razorpayPaymentId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            com.ecommerce.entity.Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment == null) {
                com.ecommerce.entity.Order order = orderService.findById(orderId);
                if (order == null) {
                    throw new RuntimeException("Order not found");
                }
                payment = new com.ecommerce.entity.Payment();
                payment.setOrder(order);
                payment.setAmount(order.getTotalAmount());
                payment.setRazorpayOrderId("mock_order_" + order.getId());
            }
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus("DISPUTED");
            paymentRepository.save(payment);
            
            response.put("success", true);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/confirm")
    public String confirmOrder(
            @RequestParam("orderId") Long orderId,
            @RequestParam(value = "razorpayOrderId", required = false) String razorpayOrderId,
            @RequestParam(value = "razorpayPaymentId", required = false) String razorpayPaymentId,
            @RequestParam(value = "razorpaySignature", required = false) String razorpaySignature,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            if (razorpayOrderId != null && razorpayPaymentId != null) {
                boolean verified = razorpayService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
                if (verified) {
                    orderService.clearCartAfterPayment(orderId);
                    redirectAttributes.addFlashAttribute("success", "Payment successful! Order placed successfully.");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Payment verification failed. Please try again.");
                }
            } else {
                orderService.updateOrderStatus(orderId, "PAID");
                orderService.clearCartAfterPayment(orderId);
                redirectAttributes.addFlashAttribute("success", "Order placed successfully! Thank you for shopping with us.");
            }
            Order confirmedOrder = orderService.findByIdSimple(orderId);
            if (confirmedOrder != null && confirmedOrder.getCouponCode() != null) {
                Coupon usedCoupon = couponRepository.findByCode(confirmedOrder.getCouponCode()).orElse(null);
                if (usedCoupon != null) {
                    usedCoupon.setUsedCount(usedCoupon.getUsedCount() != null ? usedCoupon.getUsedCount() + 1 : 1);
                    couponRepository.save(usedCoupon);
                }
            }
            session.removeAttribute("currentOrderId");
            session.removeAttribute("appliedCouponCode");
            session.removeAttribute("discountAmount");
            return "redirect:/customer/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error placing order: " + e.getMessage());
            return "redirect:/customer/orders";
        }
    }
}
