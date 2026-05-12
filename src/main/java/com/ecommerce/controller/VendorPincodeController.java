package com.ecommerce.controller;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VendorPincode;
import com.ecommerce.repository.VendorPincodeRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor/pincodes")
@RequiredArgsConstructor
public class VendorPincodeController {

    private final VendorPincodeRepository pincodeRepository;
    private final UserService userService;

    @GetMapping
    public String listPincodes(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<VendorPincode> pincodes = pincodeRepository.findByVendorId(userDetails.getId());
        model.addAttribute("pincodes", pincodes);
        return "vendor/pincodes/list";
    }

    @PostMapping("/add")
    public String addPincodes(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam("pincodeInput") String pincodeInput,
                             RedirectAttributes redirectAttributes) {
        User vendor = userService.findById(userDetails.getId());
        
        if (pincodeInput == null || pincodeInput.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pincode input cannot be empty.");
            return "redirect:/vendor/pincodes";
        }

        // Support comma or space separated inputs to make it easier for the user
        List<String> newPins = Arrays.stream(pincodeInput.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        int addedCount = 0;
        for (String pin : newPins) {
            if (!pincodeRepository.existsByVendorAndPincode(vendor, pin)) {
                VendorPincode vp = new VendorPincode();
                vp.setVendor(vendor);
                vp.setPincode(pin);
                pincodeRepository.save(vp);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            redirectAttributes.addFlashAttribute("success", "Successfully registered " + addedCount + " new delivery zone(s).");
        } else {
            redirectAttributes.addFlashAttribute("warning", "No new locations were added. They might already be in your coverage list.");
        }

        return "redirect:/vendor/pincodes";
    }

    @PostMapping("/delete/{id}")
    public String deletePincode(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {
        
        pincodeRepository.findById(id).ifPresent(pin -> {
            // Verify ownership security before deletion
            if (pin.getVendor().getId().equals(userDetails.getId())) {
                pincodeRepository.delete(pin);
                redirectAttributes.addFlashAttribute("success", "Coverage for location " + pin.getPincode() + " has been removed.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access attempt denied.");
            }
        });
        
        return "redirect:/vendor/pincodes";
    }
}
