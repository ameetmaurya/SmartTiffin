package com.example.NutriDeliver.config;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("STUDENT")) {
            response.sendRedirect("/customer/home");
        } else if (roles.contains("HUB")) {
            response.sendRedirect("/hub/dashboard");
        } else if (roles.contains("DELIVERY_PARTNER")) {
            response.sendRedirect("/delivery/schedule");
        }
        else if (roles.contains("ADMIN") || roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard"); // COMBINED ADMIN + HUB PANEL
        } else if (roles.contains("COOK")) {
            response.sendRedirect("/cook/dashboard");
        } else {
            response.sendRedirect("/login"); // Redirect back to login if role unknown
        }
    }
}