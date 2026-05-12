package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final com.ecommerce.repository.AddressRepository addressRepository;
    private final com.ecommerce.repository.UserRepository userRepository;
    private final com.ecommerce.repository.PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @PostMapping
    public String processCheckout(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.createOrderFromCart(userDetails.getId());
            Payment payment = razorpayService.createRazorpayOrder(order);
            java.util.List<com.ecommerce.entity.Address> addresses = addressRepository.findByUserId(userDetails.getId());
            
            model.addAttribute("order", order);
            model.addAttribute("payment", payment);
            model.addAttribute("razorpayKeyId", keyId);
            model.addAttribute("userFullName", userDetails.getFullName());
            model.addAttribute("userEmail", userDetails.getUsername());
            model.addAttribute("addresses", addresses);
            return "customer/checkout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing checkout: " + e.getMessage());
            return "redirect:/cart";
        }
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
            RedirectAttributes redirectAttributes) {
        try {
            if (razorpayOrderId != null && razorpayPaymentId != null) {
                boolean verified = razorpayService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
                if (verified) {
                    redirectAttributes.addFlashAttribute("success", "Payment successful! Order placed successfully.");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Payment verification failed. Please try again.");
                }
            } else {
                orderService.updateOrderStatus(orderId, "PAID");
                redirectAttributes.addFlashAttribute("success", "Order placed successfully! Thank you for shopping with us.");
            }
            return "redirect:/customer/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error placing order: " + e.getMessage());
            return "redirect:/customer/orders";
        }
    }
}
