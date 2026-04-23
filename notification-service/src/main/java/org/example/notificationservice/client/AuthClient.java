package org.example.notificationservice.client;

import java.util.UUID;

import org.example.notificationservice.config.FeignConfig;
import org.example.shared.dtos.ApiResponse;
import org.example.shared.dtos.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service",
    configuration = FeignConfig.class
)
public interface AuthClient {

    @GetMapping("/auth/users/{userId}")
    ApiResponse<UserResponse> findUserById(@PathVariable("userId") UUID userId);
}