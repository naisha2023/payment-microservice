package org.example.walletservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .x509(x509 -> x509
                        .x509PrincipalExtractor(cert -> {
                            String dn = cert.getSubjectX500Principal().getName();

                            for (String part : dn.split(",")) {
                                if (part.trim().startsWith("CN=")) {
                                    return part.trim().substring(3);
                                }
                            }

                            throw new UsernameNotFoundException("CN no encontrado en certificado");
                        })
                        .userDetailsService(mtlsUserDetailsService())
                )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**"
                ).permitAll()
                .anyRequest().hasRole("SYSTEM_SERVICE")
            )
            .build();
    }

    @Bean
    @Qualifier("mtlsUserDetailsService")
    public UserDetailsService mtlsUserDetailsService() {
        return cn -> {
            if ("payment-service".equals(cn)) {
                return User.withUsername(cn)
                    .password("")
                    .roles("SYSTEM_SERVICE")
                    .build();
            }
            throw new UsernameNotFoundException("Certificado no autorizado: " + cn);
        };
    }
}