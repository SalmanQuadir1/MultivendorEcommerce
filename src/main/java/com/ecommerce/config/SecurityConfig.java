package com.ecommerce.config;

import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.RoleBasedLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final RoleBasedLoginSuccessHandler roleBasedLoginSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.access.hierarchicalroles.RoleHierarchy roleHierarchy() {
        String hierarchyDefinition = 
            "ROLE_SUPER_ADMIN > ROLE_ADMIN\n" +
            "ROLE_ADMIN > ROLE_FINANCE_ADMIN\n" +
            "ROLE_ADMIN > ROLE_SUPPORT_ADMIN\n" +
            "ROLE_ADMIN > ROLE_CATALOG_ADMIN\n" +
            "ROLE_ADMIN > ROLE_MARKETING_ADMIN\n" +
            "ROLE_VENDOR > ROLE_INVENTORY_MANAGER\n" +
            "ROLE_VENDOR > ROLE_ORDER_MANAGER\n" +
            "ROLE_VENDOR > ROLE_VENDOR_MARKETING\n" +
            "ROLE_VENDOR > ROLE_VENDOR_SUPPORT";

        return org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl.fromHierarchy(hierarchyDefinition);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/payments/webhook", "/checkout/**", "/customer/wishlist/**", "/customer/review/**"))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "CATALOG_ADMIN", "MARKETING_ADMIN")
                .requestMatchers("/vendor/**").hasAnyRole("VENDOR", "INVENTORY_MANAGER", "ORDER_MANAGER", "VENDOR_MARKETING", "VENDOR_SUPPORT")
                .requestMatchers("/delivery/**").hasRole("DELIVERY_PARTNER")
                .requestMatchers("/warehouse/**").hasRole("WAREHOUSE_MANAGER")
                .requestMatchers("/finance/**").hasRole("FINANCE_ACCOUNTANT")
                .requestMatchers("/customer/**", "/cart/**", "/checkout/**").hasAnyRole("CUSTOMER", "ADMIN", "VENDOR", "SUPER_ADMIN")
                .requestMatchers("/", "/register", "/login", "/products/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/api/payments/webhook", "/customer/wishlist/toggle/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedLoginSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
