package org.example.notificationservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final RestClient restClient;

    @Value("${auth-service.base-url}")
    private String authBaseUrl;

    private volatile String cachedToken;

    public synchronized String getToken() {
        if (cachedToken == null) {
            cachedToken = restClient.post()
                    .uri(authBaseUrl + "/auth/internal/token")
                    .retrieve()
                    .body(String.class);
        }
        return cachedToken;
    }
}