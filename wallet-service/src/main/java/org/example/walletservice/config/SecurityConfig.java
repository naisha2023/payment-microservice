package org.example.walletservice.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ✅ mTLS
            .x509(x509 -> x509
                .x509PrincipalExtractor(cert -> {
                    String dn =
                        cert.getSubjectX500Principal().getName();

                    for (String part : dn.split(",")) {
                        if (part.trim().startsWith("CN=")) {
                            return part.trim().substring(3);
                        }
                    }

                    return null;
                })

                .userDetailsService(mtlsUserDetailsService())
            )

            // ✅ JWT
            .oauth2ResourceServer(oauth ->
                oauth.jwt(Customizer.withDefaults())
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/actuator/prometheus"
                ).permitAll()

                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        SecretKeySpec secretKey =
            new SecretKeySpec(
                jwtSecret.getBytes(),
                "HmacSHA256"
            );

        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .build();
    }

    @Bean
    public UserDetailsService mtlsUserDetailsService() {

        return cn -> {

            if ("payment-service".equals(cn) || "api-gateway".equals(cn)) {

                return User.withUsername(cn)
                    .password("")
                    .roles("SYSTEM_SERVICE")
                    .build();
            }

            throw new RuntimeException(
                "Certificado no autorizado: " + cn
            );
        };
    }
}