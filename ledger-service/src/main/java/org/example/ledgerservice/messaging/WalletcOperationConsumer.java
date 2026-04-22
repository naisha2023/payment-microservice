package org.example.ledgerservice.messaging;

import java.math.BigDecimal;
import org.example.ledgerservice.entity.LedgerAccount;
import org.example.ledgerservice.repository.LedgerAccountRepository;
import org.example.ledgerservice.service.LedgerTransactionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.example.shared.config.RabbitConfig;
import org.example.shared.event.WalletCreatedEvent;
import org.example.shared.event.WalletFundedEvent;
import org.example.shared.event.WalletReleaseFundedEvent;
import org.example.shared.enums.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletcOperationConsumer {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerTransactionService ledgerTransactionService;

    @RabbitListener(queues = RabbitConfig.WALLET_CREATED_QUEUE)
    @Transactional
    public void handle(WalletCreatedEvent event) {

        
        log.info("WalletCreatedEvent recibido para wallet {}", event.walletId());

        boolean exists = ledgerAccountRepository.findByWalletId(event.walletId()).isPresent();
        if (exists) {
            log.info("LedgerAccount ya existe para wallet {}", event.walletId());
            return;
        }

        ledgerTransactionService.createAccount(
                event.walletId(),
                event.accountType() != null ? event.accountType() : AccountType.CUSTOMER,
                event.currency(),
                event.initialBalance() != null ? event.initialBalance() : java.math.BigDecimal.ZERO
        );

        log.info("LedgerAccount creada para wallet {}", event.walletId());
    }

    @RabbitListener(queues = RabbitConfig.WALLET_FUNDED_QUEUE)
    @Transactional
    public void handle(WalletFundedEvent event) {

        log.info("WalletFundedEvent recibido: wallet {}, amount {}", event.walletId(), event.amount());

        LedgerAccount account = ledgerAccountRepository.findByWalletId(event.walletId())
                .orElseThrow(() -> new IllegalStateException("LedgerAccount no existe"));

        BigDecimal before = account.getAvailableBalance();

        account.setAvailableBalance(before.add(event.amount()));
        ledgerAccountRepository.save(account);

        ledgerTransactionService.createEntry(event);

        log.info("Fondos registrados en ledger para wallet {}", event.walletId());
    }

    @RabbitListener(queues = RabbitConfig.WALLET_RELEASE_FUNDED_QUEUE)
    @Transactional
    public void handle(WalletReleaseFundedEvent event) {

        log.info("WalletReleaseFundedEvent recibido: wallet {}, amount {}", event.walletId(), event.amount());

        LedgerAccount account = ledgerAccountRepository.findByWalletId(event.walletId())
                .orElseThrow(() -> new IllegalStateException("LedgerAccount no existe"));

        BigDecimal before = account.getAvailableBalance();

        account.setAvailableBalance(before.add(event.amount()));
        account.setReservedBalance(account.getReservedBalance().subtract(event.amount()));
        ledgerAccountRepository.save(account);

        ledgerTransactionService.createEntry(event);

        log.info("Fondos liberados en ledger para wallet {}", event.walletId());
    }
}