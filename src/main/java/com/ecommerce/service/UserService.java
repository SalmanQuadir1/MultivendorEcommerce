package com.ecommerce.service;

import com.ecommerce.dto.UserRegistrationDto;
import com.ecommerce.entity.User;

public interface UserService {
    User save(UserRegistrationDto registrationDto);
    User findByEmail(String email);
    User findById(Long id);
    void updateProfile(Long userId, String fullName, String logoUrl);
}
