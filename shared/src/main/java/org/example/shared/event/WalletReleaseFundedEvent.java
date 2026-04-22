package org.example.shared.event;

import java.math.BigDecimal;
import java.util.UUID;

import org.example.shared.interfaces.LedgerEvent;

public record WalletReleaseFundedEvent(UUID walletId,
        UUID userId,
        UUID eventid,
        UUID referenceId,
        BigDecimal amount,
        String currency,
        String reason) implements LedgerEvent{

        @Override
        public UUID getWalletId() {
                return walletId;
        }

        @Override
        public BigDecimal getAmount() {
                return amount;
        }

        @Override
        public String getCurrency() {
                return currency;
        }

        @Override
        public String getReferenceId() {
                return referenceId.toString();
        }

        @Override
        public String getEventType() {
                return "WALLET_FUNDED";
        }

        @Override
        public String getReazon() {
                return reason;
        }
}
