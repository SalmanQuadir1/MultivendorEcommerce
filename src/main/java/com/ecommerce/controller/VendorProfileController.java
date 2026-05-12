package com.ecommerce.controller;

import com.ecommerce.entity.User;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.FileUploadService;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/vendor/profile")
@RequiredArgsConstructor
public class VendorProfileController {

    private final UserService userService;
    private final FileUploadService fileUploadService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userService.findById(userDetails.getId());
        model.addAttribute("user", user);
        return "vendor/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam("fullName") String fullName,
                                @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                                RedirectAttributes redirectAttributes) {
        try {
            String logoUrl = null;
            if (logoFile != null && !logoFile.isEmpty()) {
                logoUrl = fileUploadService.uploadFile(logoFile);
            }

            userService.updateProfile(userDetails.getId(), fullName, logoUrl);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully! Changes will reflect in real-time.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload logo image: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
        }

        return "redirect:/vendor/profile";
    }
}
