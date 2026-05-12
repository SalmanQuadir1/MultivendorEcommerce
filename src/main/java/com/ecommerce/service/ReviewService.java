package com.ecommerce.service;

import com.ecommerce.entity.Review;
import java.util.List;

public interface ReviewService {
    Review addReview(Long productId, String userEmail, Integer rating, String comment);
    List<Review> getReviewsByProduct(Long productId);
    Double getAverageRating(Long productId);
}
