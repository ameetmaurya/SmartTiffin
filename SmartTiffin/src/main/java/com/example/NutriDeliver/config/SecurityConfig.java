package com.example.NutriDeliver.config;

import com.example.NutriDeliver.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomSuccessHandler customSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**",
                                 "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/customer/**").hasAuthority("STUDENT")
                .requestMatchers("/cook/**").hasAuthority("COOK")
                .requestMatchers("/delivery/**").hasAuthority("DELIVERY_PARTNER")
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler)
                .permitAll()
            )

            // FIX: explicit entry point so unauthenticated requests always go
            // to /login, never to /register or any other page
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendRedirect("/login"))
            )

            // FIX: role-aware logout — each role lands back on /login?logout
            // with the "signed out" banner shown. We can't redirect back to the
            // protected dashboard because Spring Security has already cleared the
            // session — the user IS logged out and can't access /delivery/schedule.
            // The correct UX is: log out → login page → sign back in → dashboard.
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    // authentication holds the role BEFORE logout
                    if (authentication != null) {
                        Set<String> roles = AuthorityUtils
                            .authorityListToSet(authentication.getAuthorities());

                        if (roles.contains("DELIVERY_PARTNER")) {
                            response.sendRedirect("/login?logout&role=delivery");
                        } else if (roles.contains("COOK")) {
                            response.sendRedirect("/login?logout&role=cook");
                        } else if (roles.contains("STUDENT")) {
                            response.sendRedirect("/login?logout&role=student");
                        } else if (roles.contains("ADMIN")) {
                            response.sendRedirect("/login?logout&role=admin");
                        } else {
                            response.sendRedirect("/login?logout");
                        }
                    } else {
                        response.sendRedirect("/login?logout");
                    }
                })
                .permitAll()
            );

        return http.build();
    }
}