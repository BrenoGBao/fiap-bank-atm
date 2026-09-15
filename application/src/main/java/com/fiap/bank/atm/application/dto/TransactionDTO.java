package com.fiap.bank.atm.application.dto;

import com.fiap.bank.atm.domain.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO (Java Record) para uma linha do extrato. Expõe o tipo da transação como
 * {@code String} (o nome do enum) em vez do enum de domínio, e o valor como
 * {@code BigDecimal} em vez do Value Object {@code Money}, mantendo a camada
 * de aplicação isolada dos tipos internos do domínio.
 */
public record TransactionDTO(
        UUID id,
        LocalDateTime timestamp,
        String type,
        String typeDescription,
        BigDecimal amount,
        String description) {

    public static TransactionDTO from(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getTimestamp(),
                transaction.getType().name(),
                transaction.getType().getDescription(),
                transaction.getAmount().getAmount(),
                transaction.getDescription());
    }
}
