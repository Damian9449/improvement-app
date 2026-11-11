package com.improvement_app.food.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class PermitAllSecurityConfig {

    // Łańcuch testowy musi wygrać z produkcyjnym (WebSecurityConfig). Spring Security 6.5 odrzuca
    // dwa łańcuchy "anyRequest", więc dopasowujemy "/**" jawnie — działa tak samo, ale nie jest
    // traktowane jako drugi łańcuch "any request" i nie blokuje startu kontekstu.
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults()); // opcjonalnie
        return http.build();
    }

}
