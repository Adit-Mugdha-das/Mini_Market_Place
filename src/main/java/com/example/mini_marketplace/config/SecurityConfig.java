package com.example.mini_marketplace.config;

import com.example.mini_marketplace.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setPreAuthenticationChecks(userDetails -> {
            boolean isBuyerOrSeller = userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_BUYER".equals(authority.getAuthority())
                    || "ROLE_SELLER".equals(authority.getAuthority()));

            if (!userDetails.isAccountNonLocked()) {
                throw new LockedException("User account is locked");
            }
            if (!userDetails.isAccountNonExpired()) {
                throw new AccountExpiredException("User account has expired");
            }
            if (!userDetails.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException("User credentials have expired");
            }
            if (!userDetails.isEnabled() && !isBuyerOrSeller) {
                throw new DisabledException("User is disabled");
            }
        });
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**", "/products/**", "/css/**", "/js/**", "/images/**", "/uploads/products/**", "/webjars/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/profile/**").authenticated()
                .requestMatchers("/seller/**").hasAnyRole("ADMIN", "SELLER")
                .requestMatchers("/buyer/**").hasAnyRole("ADMIN", "BUYER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(successHandler())
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(rememberMe -> rememberMe
                .key("haatBazarSecretKey")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                .rememberMeParameter("remember-me")
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            var authorities = authentication.getAuthorities();
            String redirectUrl = "/buyer/products";
            boolean accountEnabled = true;
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                accountEnabled = userDetails.isEnabled();
            }

            for (var authority : authorities) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    redirectUrl = "/admin/dashboard";
                    break;
                } else if ("ROLE_SELLER".equals(role)) {
                    redirectUrl = accountEnabled ? "/seller/dashboard" : "/seller/profile";
                    break;
                } else if ("ROLE_BUYER".equals(role)) {
                    redirectUrl = "/buyer/products";
                }
            }
            response.sendRedirect(redirectUrl);
        };
    }
}
