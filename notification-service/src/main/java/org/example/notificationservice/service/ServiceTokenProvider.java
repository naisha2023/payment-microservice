package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final RestClient restClient;

    private volatile String cachedToken;

    public synchronized String getToken() {
        if (cachedToken == null) {
            cachedToken = restClient.post()
                    .uri("https://auth-service/auth/internal/token")
                    .retrieve()
                    .body(String.class);
        }
        return cachedToken;
    }
}