package org.example.paymentservice.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.example.paymentservice.dto.WalletOperationRequest;
import org.example.paymentservice.dto.WalletResponse;

@FeignClient(name = "wallet-service")
public interface WalletClient {
        @PostMapping("/wallets/{userId}/reserve") 
        void reserve(
                @PathVariable("userId") UUID userId,
                @RequestBody WalletOperationRequest request,
                @RequestHeader("Authorization") String authHeader
        );

        @PostMapping("/wallets/{userId}/confirm-debit")
        void confirmDebit(
                @PathVariable("userId") UUID userId,
                @RequestBody WalletOperationRequest request,
                @RequestHeader("Authorization") String authHeader
        );

        @PostMapping("/wallets/{userId}/release")
        void release(
                @PathVariable("userId") UUID userId,
                @RequestBody WalletOperationRequest request,
                @RequestHeader("Authorization") String authHeader
        );   

        @GetMapping("/wallets/me")
        org.example.shared.dtos.ApiResponse<WalletResponse> getMyWallet(
        @RequestHeader("Authorization") String authHeader
        );
} 