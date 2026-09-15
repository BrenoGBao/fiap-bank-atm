package com.fiap.bank.atm;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.config.ConnectionFactory;
import com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl;
import com.fiap.bank.atm.presentation.AtmFrame;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        // Fase 4: cria/abre o banco SQLite e garante que o schema exista.
        ConnectionFactory.initializeSchema();

        AccountRepository accountRepository = new AccountRepositoryJdbcImpl();
        seedIfEmpty(accountRepository);

        AtmService atmService = new AtmService(accountRepository);

        // Inicializa a camada de Apresentação de forma segura na Event Dispatch
        // Thread (EDT). A classe AtmFrame não foi alterada em nenhum caractere.
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }

    /**
     * Na primeira execução (banco recém-criado, sem nenhuma conta), popula as
     * mesmas 3 contas de teste que o projeto original mantinha em memória -
     * agora persistidas de fato no SQLite.
     */
    private static void seedIfEmpty(AccountRepository accountRepository) {
        if (!accountRepository.buscarTodos().isEmpty()) {
            return;
        }

        Account acc1 = new Account(UUID.randomUUID(), "12345", "1234", Money.of(5000.00), Money.of(1500.00));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(3),
                TransactionType.DEPOSIT, Money.of(2000.00), "Depósito em dinheiro"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_IN, Money.of(500.00), "Transf. de Conta 67890"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(1),
                TransactionType.WITHDRAWAL, Money.of(100.00), "Saque eletrônico"));
        accountRepository.salvar(acc1);

        Account acc2 = new Account(UUID.randomUUID(), "67890", "5678", Money.of(1200.00), Money.of(1000.00));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(5),
                TransactionType.DEPOSIT, Money.of(1500.00), "Depósito inicial"));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_OUT, Money.of(500.00), "Transf. para Conta 12345"));
        accountRepository.salvar(acc2);

        Account acc3 = new Account(UUID.randomUUID(), "99999", "9999", Money.of(50.00), Money.of(500.00));
        acc3.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(10),
                TransactionType.DEPOSIT, Money.of(50.00), "Abertura de conta"));
        accountRepository.salvar(acc3);
    }
}
