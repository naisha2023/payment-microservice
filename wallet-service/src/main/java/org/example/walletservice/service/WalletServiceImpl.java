package org.example.walletservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.enums.AccountType;
import org.example.shared.event.WalletCreatedEvent;
import org.example.shared.event.WalletDebitConfirmedEvent;
import org.example.shared.event.WalletFundedEvent;
import org.example.shared.event.WalletReleaseFundedEvent;
import org.example.walletservice.constants.WalletConstants;
import org.example.walletservice.dto.*;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.entity.WalletOperation;
import org.example.walletservice.enums.WalletOperationType;
import org.example.walletservice.enums.WalletStatus;
import org.example.walletservice.exception.BusinessException;
import org.example.walletservice.exception.InsufficientFundsException;
import org.example.walletservice.exception.ResourceNotFoundException;
import org.example.walletservice.repository.WalletOperationRepository;
import org.example.walletservice.repository.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.walletservice.messaging.WalletEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementación del servicio de Wallet
 * Maneja operaciones de wallet con soporte para transacciones idempotentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletOperationRepository walletOperationRepository;
    private final WalletEventPublisher walletEventPublisher;

    @Override
    public WalletResponse getWalletForUser(UUID walletId, UUID userId) {
        log.info("Obteniendo wallet para usuario: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        WalletConstants.ERROR_WALLET_NOT_FOUND + ": " + walletId));
        return toResponse(wallet);
    }

    @Override
    public WalletResponse getWallet(UUID userId) {
        log.info("Obteniendo wallet por userId: {}", userId);
        Wallet wallet = findWalletByUserId(userId);
        return toResponse(wallet);
    }

    @Override
    public BalanceResponse getBalance(UUID userId) {
        log.info("Obteniendo balance para usuario: {}", userId);
        Wallet wallet = findWalletByUserId(userId);
        return new BalanceResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getReservedBalance(),
                wallet.availableBalance());
    }

    @Override
    @Transactional
    public WalletResponse reserve(UUID userId, ReserveFundsRequest request) {
        log.info("Reservando fondos para usuario: {}, monto: {}, paymentId: {}",
                userId, request.amount(), request.paymentId());

        Wallet wallet = findWalletByUserId(userId);

        WalletOperationResult result = executeIdempotentOperation(
            wallet.getId(),
            request.paymentId(),
            WalletOperationType.RESERVE,
            request.amount(),
            w -> reserveFunds(w, request.amount()));
        return result.wallet();
    }

    @Override
    @Transactional
    public WalletResponse release(UUID userId, ReleaseFundsRequest request) {
        log.info("Liberando fondos para usuario: {}, monto: {}, paymentId: {}",
                userId, request.amount(), request.paymentId());

        Wallet wallet = findWalletByUserId(userId);

       
        WalletOperationResult result = executeIdempotentOperation(
            wallet.getId(),
            request.paymentId(),
            WalletOperationType.RELEASE,
            request.amount(),
            w -> releaseFunds(w, wallet.getId(), request));

        if(result.newlyProcessed()) {
            walletEventPublisher.publish(
                new WalletReleaseFundedEvent(
                    wallet.getId(),
                    userId,
                    UUID.randomUUID(),
                    request.paymentId(),
                    request.amount(),
                    wallet.getCurrency(),
                    "RELEASE FOUNDS CONFIRMED BY WALLET SERVICE API FOR USER " + userId
                )
            );
        }
        return result.wallet();
    }

    @Override
    @Transactional
    public WalletResponse confirmDebit(UUID userId, ConfirmDebitRequest request) {
        log.info("Confirmando débito para usuario: {}, monto: {}, paymentId: {}",
            userId, request.amount(), request.paymentId());

        Wallet wallet = findWalletByUserId(userId);

        WalletOperationResult result = executeIdempotentOperation(
            wallet.getId(),
            request.paymentId(),
            WalletOperationType.CONFIRM_DEBIT,
            request.amount(),
            w -> confirmDebitFunds(w, wallet.getId(), request));
        
        if (result.newlyProcessed()) {
            walletEventPublisher.publish(
                new WalletDebitConfirmedEvent(
                    wallet.getId(),
                        userId,
                        request.paymentId(),
                        request.amount(),
                        wallet.getCurrency(),
                        "DEBIT CONFIRMED BY WALLET SERVICE API FOR USER " + userId
                )
            );
        }
        return result.wallet();

    }

    @Override
    @Transactional
    public WalletResponse credit(UUID userId, CreditRequest request) {
        log.info("Acreditando fondos para usuario: {}, monto: {}, paymentId: {}",
                userId, request.amount(), request.paymentId());

        Wallet wallet = findWalletByUserId(userId);

        WalletOperationResult result = executeIdempotentOperation(
            wallet.getId(),
            request.paymentId(),
            WalletOperationType.CREDIT,
            request.amount(),
            w -> w.setBalance(w.getBalance().add(request.amount()))
        );

        // publicar solo si esta operación fue realmente aplicada por primera vez
        if (result.newlyProcessed())
            walletEventPublisher.publish(
            new WalletFundedEvent(
                UUID.randomUUID(),
                wallet.getId(),
                userId,
                request.paymentId(),
                request.amount(),
                wallet.getCurrency(),
                "CREDIT"
            )
        );

        return result.wallet();
    }

    @Transactional
    public void createWalletIfNotExists(UUID userId) {
        log.info("Verificando/creando wallet para usuario: {}", userId);

        if (walletRepository.findByUserId(userId).isPresent()) {
            log.debug("Wallet ya existe para usuario: {}", userId);
            return;
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .currency(WalletConstants.DEFAULT_CURRENCY)
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);
        walletEventPublisher.publish(new WalletCreatedEvent(wallet.getId(),AccountType.CUSTOMER, wallet.getCurrency(), wallet.getBalance(), userId));
        log.info("Wallet creada exitosamente para usuario: {}", userId);
    }

    // ==================== Métodos privados de operaciones ====================

    private WalletOperationResult executeIdempotentOperation(
        UUID walletId,
        UUID paymentId,
        WalletOperationType operationType,
        BigDecimal amount,
        WalletMutation mutation) {

        validatePositiveAmount(amount);

        Wallet wallet = findWallet(walletId);
        validateWalletIsActive(wallet);

        if (isOperationAlreadyExecuted(walletId, paymentId, operationType)) {
            log.info("Operación {} ya ejecutada para payment: {}, retornando wallet actual",
                    operationType, paymentId);
            return new WalletOperationResult(toResponse(wallet), false);
        }

        mutation.apply(wallet);

        Wallet savedWallet = saveWalletWithOperation(wallet, walletId, paymentId, operationType, amount);

        log.info("Operación {} completada exitosamente para payment: {}", operationType, paymentId);
        return new WalletOperationResult(toResponse(savedWallet), true);
    }

    private Wallet saveWalletWithOperation(
            Wallet wallet,
            UUID walletId,
            UUID paymentId,
            WalletOperationType operationType,
            BigDecimal amount) {
        try {
            Wallet savedWallet = walletRepository.saveAndFlush(wallet);

            WalletOperation operation = WalletOperation.builder()
                    .walletId(walletId)
                    .paymentId(paymentId)
                    .operationType(operationType)
                    .amount(amount)
                    .build();

            walletOperationRepository.saveAndFlush(operation);

            return savedWallet;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violación de integridad detectada, operación duplicada para payment: {}", paymentId);
            return findWallet(walletId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error("Fallo de bloqueo optimista para wallet: {}", walletId);
            throw new BusinessException(WalletConstants.ERROR_CONCURRENT_UPDATE);
        }
    }

    private void reserveFunds(Wallet wallet, BigDecimal amount) {
        BigDecimal available = wallet.availableBalance();
        if (available.compareTo(amount) < 0) {
            log.warn("Fondos insuficientes. Disponible: {}, Requerido: {}", available, amount);
            throw new InsufficientFundsException(WalletConstants.ERROR_INSUFFICIENT_AVAILABLE_FUNDS);
        }
        wallet.setReservedBalance(wallet.getReservedBalance().add(amount));
    }

    private void releaseFunds(Wallet wallet, UUID walletId, ReleaseFundsRequest request) {
        validateReserveFlow(walletId, request.paymentId());

        if (wallet.getReservedBalance().compareTo(request.amount()) < 0) {
            log.error("Fondos reservados insuficientes para liberar. Reservado: {}, Solicitado: {}",
                    wallet.getReservedBalance(), request.amount());
            throw new BusinessException(WalletConstants.ERROR_INSUFFICIENT_RESERVED_FUNDS);
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.amount()));
    }

    private void confirmDebitFunds(Wallet wallet, UUID walletId, ConfirmDebitRequest request) {
        validateReserveFlow(walletId, request.paymentId());

        if (wallet.getReservedBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(WalletConstants.ERROR_INSUFFICIENT_RESERVED_FUNDS);
        }

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(WalletConstants.ERROR_INSUFFICIENT_FUNDS);
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.amount()));
        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
    }

    // ==================== Métodos privados de validación ====================

    private void validateWalletIsActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Intento de operación en wallet inactiva: {}", wallet.getId());
            throw new BusinessException(WalletConstants.ERROR_WALLET_NOT_ACTIVE);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Monto inválido recibido: {}", amount);
            throw new BusinessException(WalletConstants.ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
    }

    private void validateReserveFlow(UUID walletId, UUID paymentId) {
        validateReserveExists(walletId, paymentId);
        validateNotAlreadyConfirmed(walletId, paymentId);
    }

    private void validateReserveExists(UUID walletId, UUID paymentId) {
        boolean reserved = walletOperationRepository
                .existsByWalletIdAndPaymentIdAndOperationType(
                        walletId, paymentId, WalletOperationType.RESERVE);

        if (!reserved) {
            log.error("No se encontró reserva para payment: {}", paymentId);
            throw new BusinessException(WalletConstants.ERROR_NO_RESERVE_FOUND + ": " + paymentId);
        }
    }

    private void validateNotAlreadyConfirmed(UUID walletId, UUID paymentId) {
        boolean confirmed = walletOperationRepository
                .existsByWalletIdAndPaymentIdAndOperationType(
                        walletId,
                        paymentId,
                        WalletOperationType.CONFIRM_DEBIT);

        if (confirmed) {
            log.warn("Intento de confirmar payment ya confirmado: {}", paymentId);
            throw new BusinessException(WalletConstants.ERROR_PAYMENT_ALREADY_CONFIRMED + ": " + paymentId);
        }
    }

    private boolean isOperationAlreadyExecuted(
            UUID walletId,
            UUID paymentId,
            WalletOperationType operationType) {
        return walletOperationRepository.existsByWalletIdAndPaymentIdAndOperationType(
                walletId, paymentId, operationType);
    }

    // ==================== Métodos privados de búsqueda ====================

    private Wallet findWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        WalletConstants.ERROR_WALLET_NOT_FOUND + ": " + walletId));
    }

    private Wallet findWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        WalletConstants.ERROR_WALLET_NOT_FOUND_FOR_USER + ": " + userId));
    }

    // ==================== Métodos privados de conversión ====================

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getCurrency(),
                wallet.getBalance());
    }

    /**
     * Interfaz funcional para mutaciones de wallet
     */
    @FunctionalInterface
    private interface WalletMutation {
        void apply(Wallet wallet);
    }
}
