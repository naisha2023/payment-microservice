package org.example.walletservice.service;

import org.example.walletservice.dto.BalanceResponse;
import org.example.walletservice.dto.ConfirmDebitRequest;
import org.example.walletservice.dto.CreditRequest;
import org.example.walletservice.dto.ReleaseFundsRequest;
import org.example.walletservice.dto.ReserveFundsRequest;
import org.example.walletservice.dto.WalletResponse;

import java.util.UUID;

public interface WalletService {
    WalletResponse getWallet(UUID userId);
    BalanceResponse getBalance(UUID userId);
    WalletResponse reserve(UUID userId, ReserveFundsRequest request);
    WalletResponse credit(UUID userId, CreditRequest request);
    WalletResponse release(UUID userId, ReleaseFundsRequest request);
    WalletResponse confirmDebit(UUID userId, ConfirmDebitRequest request);
    WalletResponse getWalletForUser(UUID walletId, UUID userId);
    void createWalletIfNotExists(UUID userId);
}
