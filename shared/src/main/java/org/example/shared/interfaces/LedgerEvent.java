package org.example.shared.interfaces;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerEvent {
    UUID getWalletId();
    BigDecimal getAmount();
    String getCurrency();
    String getReferenceId();
    String getEventType();
    String getReazon();
    String reason();
}
