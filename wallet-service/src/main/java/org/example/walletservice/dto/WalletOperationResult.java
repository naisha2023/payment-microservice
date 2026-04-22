package org.example.walletservice.dto;

public record WalletOperationResult(
        WalletResponse wallet,
        boolean newlyProcessed
) {}