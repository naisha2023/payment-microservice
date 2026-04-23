package org.example.notificationservice.config;

import org.example.notificationservice.service.ServiceTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final ServiceTokenProvider serviceTokenProvider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> template.header(
                "Authorization",
                "Bearer " + serviceTokenProvider.getToken()
        );
    }
}