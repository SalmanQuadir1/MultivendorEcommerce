package com.ecommerce.controller;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String viewAddresses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        List<Address> addresses = addressRepository.findByUserId(userDetails.getId());
        model.addAttribute("addresses", addresses);
        return "customer/addresses";
    }

    @PostMapping("/add")
    public String addAddress(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam("fullName") String fullName,
                             @RequestParam("streetAddress") String streetAddress,
                             @RequestParam("city") String city,
                             @RequestParam("state") String state,
                             @RequestParam("pinCode") String pinCode,
                             @RequestParam("contactNumber") String contactNumber,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Address address = Address.builder()
                    .user(user)
                    .fullName(fullName)
                    .streetAddress(streetAddress)
                    .city(city)
                    .state(state)
                    .pinCode(pinCode)
                    .contactNumber(contactNumber)
                    .isDefault(false)
                    .build();

            addressRepository.save(address);
            redirectAttributes.addFlashAttribute("success", "Address saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving address: " + e.getMessage());
        }
        return "redirect:/customer/addresses";
    }

    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            addressRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Address deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting address: " + e.getMessage());
        }
        return "redirect:/customer/addresses";
    }
}
