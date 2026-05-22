package org.example.authservice.config;

import org.example.authservice.entity.Users;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 🔐 FILTRO PRINCIPAL
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers("/auth/internal/**").hasRole("SYSTEM_SERVICE")
                        .requestMatchers(
                                "/auth/me",
                                "/auth/logout"
                        ).authenticated()
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
  
    @Bean
    public UserDetailsService mtlsUserDetailsService() {

        Set<String> allowedServices = Set.of(
                "notification-service",
                "wallet-service",
                "payment-service"
        );

        return cn -> {
            if (allowedServices.contains(cn)) {
                return User.withUsername(cn)
                        .password("{noop}")
                        .roles("SYSTEM_SERVICE")
                        .build();
            }

            throw new UsernameNotFoundException("Certificado no autorizado: " + cn);
        };
    }

    // 👤 USERS (JWT)
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .map(this::toUserDetails)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado")
                );
    }

    private UserDetails toUserDetails(Users user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                "ACTIVE".equalsIgnoreCase(user.getStatus())
        );
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService());

        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

  
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}