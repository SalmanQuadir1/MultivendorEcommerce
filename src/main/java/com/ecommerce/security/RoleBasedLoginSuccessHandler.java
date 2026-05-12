package com.ecommerce.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class RoleBasedLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String redirectUrl = determineRedirectUrl(authorities);
        response.sendRedirect(redirectUrl);
    }

    private String determineRedirectUrl(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority auth : authorities) {
            String role = auth.getAuthority();
            switch (role) {
                // ── Top Admin Tier ──────────────────────────────────────
                case "ROLE_SUPER_ADMIN":
                case "ROLE_ADMIN":
                case "ROLE_FINANCE_ADMIN":
                case "ROLE_SUPPORT_ADMIN":
                case "ROLE_CATALOG_ADMIN":
                case "ROLE_MARKETING_ADMIN":
                    return "/admin/dashboard";

                // ── Vendor Tier ─────────────────────────────────────────
                case "ROLE_VENDOR":
                case "ROLE_INVENTORY_MANAGER":
                case "ROLE_ORDER_MANAGER":
                case "ROLE_VENDOR_MARKETING":
                case "ROLE_VENDOR_SUPPORT":
                    return "/vendor/dashboard";

                // ── Specialized Operations ───────────────────────────────
                case "ROLE_DELIVERY_PARTNER":
                    return "/delivery/dashboard";

                case "ROLE_WAREHOUSE_MANAGER":
                    return "/warehouse/dashboard";

                case "ROLE_FINANCE_ACCOUNTANT":
                    return "/finance/dashboard";

                // ── Customer ─────────────────────────────────────────────
                case "ROLE_CUSTOMER":
                    return "/";

                default:
                    break;
            }
        }
        // Safe fallback
        return "/";
    }
}
