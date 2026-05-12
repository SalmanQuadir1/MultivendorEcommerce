package com.ecommerce.controller;

import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/customer/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/submit")
    @ResponseBody
    public Map<String, Object> submitReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Please log in to submit a review.");
            return response;
        }

        try {
            Long productId = Long.parseLong(payload.get("productId"));
            Integer rating = Integer.parseInt(payload.get("rating"));
            String comment = payload.get("comment");

            if (rating < 1 || rating > 5) {
                response.put("success", false);
                response.put("message", "Rating must be between 1 and 5.");
                return response;
            }

            reviewService.addReview(productId, userDetails.getUsername(), rating, comment);
            
            response.put("success", true);
            response.put("message", "Review submitted successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return response;
    }
}
