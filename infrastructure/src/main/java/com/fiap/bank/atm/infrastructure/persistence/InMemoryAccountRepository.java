package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória do {@link AccountRepository}. Não é mais a
 * persistência "oficial" da aplicação (esse papel passou para
 * {@link AccountRepositoryJdbcImpl}, com SQLite via JDBC), mas foi mantida
 * como alternativa leve - útil para testes unitários rápidos que não devem
 * depender de um arquivo de banco em disco.
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> accountsById = new HashMap<>();

    public InMemoryAccountRepository() {
        seedData();
    }

    private void seedData() {
        Account acc1 = new Account(UUID.randomUUID(), "12345", "1234", Money.of(5000.00), Money.of(1500.00));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(3),
                TransactionType.DEPOSIT, Money.of(2000.00), "Depósito em dinheiro"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_IN, Money.of(500.00), "Transf. de Conta 67890"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(1),
                TransactionType.WITHDRAWAL, Money.of(100.00), "Saque eletrônico"));
        accountsById.put(acc1.getId(), acc1);

        Account acc2 = new Account(UUID.randomUUID(), "67890", "5678", Money.of(1200.00), Money.of(1000.00));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(5),
                TransactionType.DEPOSIT, Money.of(1500.00), "Depósito inicial"));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_OUT, Money.of(500.00), "Transf. para Conta 12345"));
        accountsById.put(acc2.getId(), acc2);

        Account acc3 = new Account(UUID.randomUUID(), "99999", "9999", Money.of(50.00), Money.of(500.00));
        acc3.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(10),
                TransactionType.DEPOSIT, Money.of(50.00), "Abertura de conta"));
        accountsById.put(acc3.getId(), acc3);
    }

    @Override
    public Optional<Account> buscarPorId(UUID id) {
        return Optional.ofNullable(accountsById.get(id));
    }

    @Override
    public Optional<Account> buscarPorNumeroConta(String accountNumber) {
        return accountsById.values().stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst();
    }

    @Override
    public void salvar(Account entidade) {
        accountsById.put(entidade.getId(), entidade);
    }

    @Override
    public void remover(UUID id) {
        accountsById.remove(id);
    }

    @Override
    public List<Account> buscarTodos() {
        return accountsById.values().stream().toList();
    }
}
