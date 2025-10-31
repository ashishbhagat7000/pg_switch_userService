package com.vol.pgswitch.config;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ Enable CORS here
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // usually disabled for APIs
                .authorizeHttpRequests(auth -> auth
                        // allow unauthenticated access to keto admin wrapper endpoints and actuator
                        .requestMatchers("/api/keto/**", "/actuator/**").permitAll()
                        // everything else requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }


    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromJwt);
        return converter;
    }

    private List<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        log.info("JWT Subject: {}", jwt.getSubject());
        log.info("JWT Issuer: {}", jwt.getIssuer());
        log.info("JWT Claims: {}", jwt.getClaims());
        log.info("JWT Cognito Groups: {}", groups);

        if (groups == null || groups.isEmpty()) {
            log.warn("No Cognito groups found in token for user {}", jwt.getSubject());
            return List.of();
        }

        List<GrantedAuthority> authorities = groups.stream()
                .map(group -> new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                .collect(Collectors.toList());

        log.info("Mapped authorities: {}", authorities);

        return authorities;
    }
}
