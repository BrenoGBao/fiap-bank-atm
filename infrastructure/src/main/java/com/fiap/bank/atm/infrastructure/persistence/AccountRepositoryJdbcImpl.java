package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.config.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação concreta de {@link AccountRepository} usando JDBC puro
 * contra um banco SQLite (Fase 4). Respeita rigorosamente a Regra 3
 * (Proteção contra SQL Injection): toda instrução usa {@link PreparedStatement}
 * com parâmetros via métodos {@code set...}, nunca concatenação de String.
 */
public class AccountRepositoryJdbcImpl implements AccountRepository {

    private static final String SELECT_ACCOUNT_BASE = """
            SELECT id, account_number, pin, balance, daily_withdrawal_limit,
                   total_withdrawn_today, blocked, failed_attempts, created_at, updated_at
            FROM tb_account
            """;

    private static final String SELECT_TRANSACTIONS_BY_ACCOUNT = """
            SELECT id, type, amount, description, created_at
            FROM tb_transaction
            WHERE account_id = ?
            ORDER BY created_at ASC
            """;

    @Override
    public Optional<Account> buscarPorId(UUID id) {
        String sql = SELECT_ACCOUNT_BASE + " WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Account account = mapAccount(resultSet);
                    carregarTransacoes(connection, account);
                    return Optional.of(account);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar conta por id.", e);
        }
    }

    @Override
    public Optional<Account> buscarPorNumeroConta(String accountNumber) {
        String sql = SELECT_ACCOUNT_BASE + " WHERE account_number = ?";
        try (Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Account account = mapAccount(resultSet);
                    carregarTransacoes(connection, account);
                    return Optional.of(account);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar conta por número.", e);
        }
    }

    @Override
    public List<Account> buscarTodos() {
        List<Account> contas = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ACCOUNT_BASE);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Account account = mapAccount(resultSet);
                carregarTransacoes(connection, account);
                contas.add(account);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar contas.", e);
        }
        return contas;
    }

    @Override
    public void salvar(Account entidade) {
        String upsertAccount = """
                INSERT INTO tb_account
                    (id, account_number, pin, balance, daily_withdrawal_limit,
                     total_withdrawn_today, blocked, failed_attempts, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    account_number = excluded.account_number,
                    pin = excluded.pin,
                    balance = excluded.balance,
                    daily_withdrawal_limit = excluded.daily_withdrawal_limit,
                    total_withdrawn_today = excluded.total_withdrawn_today,
                    blocked = excluded.blocked,
                    failed_attempts = excluded.failed_attempts,
                    updated_at = excluded.updated_at
                """;

        String deleteTransactions = "DELETE FROM tb_transaction WHERE account_id = ?";
        String insertTransaction = """
                INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertAccount)) {
                    statement.setString(1, entidade.getId().toString());
                    statement.setString(2, entidade.getAccountNumber());
                    statement.setString(3, entidade.getPin());
                    // toPlainString() evita perda de precisão (ver comentário no ConnectionFactory)
                    statement.setString(4, entidade.getBalance().getAmount().toPlainString());
                    statement.setString(5, entidade.getDailyWithdrawalLimit().getAmount().toPlainString());
                    statement.setString(6, entidade.getTotalWithdrawnToday().getAmount().toPlainString());
                    statement.setInt(7, entidade.isBlocked() ? 1 : 0);
                    statement.setInt(8, entidade.getFailedAttempts());
                    statement.setString(9, entidade.getCreatedAt().toString());
                    statement.setString(10, entidade.getUpdatedAt().toString());
                    statement.executeUpdate();
                }

                // Reescreve o histórico de transações do zero: mais simples e à prova de
                // duplicidade do que tentar diferenciar quais já existiam no banco.
                try (PreparedStatement statement = connection.prepareStatement(deleteTransactions)) {
                    statement.setString(1, entidade.getId().toString());
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(insertTransaction)) {
                    for (Transaction transacao : entidade.getTransactions()) {
                        statement.setString(1, transacao.getId().toString());
                        statement.setString(2, entidade.getId().toString());
                        statement.setString(3, transacao.getType().name());
                        statement.setString(4, transacao.getAmount().getAmount().toPlainString());
                        statement.setString(5, transacao.getDescription());
                        statement.setString(6, transacao.getTimestamp().toString());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar conta.", e);
        }
    }

    @Override
    public void remover(UUID id) {
        String sql = "DELETE FROM tb_account WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover conta.", e);
        }
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        String accountNumber = resultSet.getString("account_number");
        String pin = resultSet.getString("pin");
        Money balance = Money.of(new BigDecimal(resultSet.getString("balance")));
        Money dailyWithdrawalLimit = Money.of(new BigDecimal(resultSet.getString("daily_withdrawal_limit")));
        Money totalWithdrawnToday = Money.of(new BigDecimal(resultSet.getString("total_withdrawn_today")));
        boolean blocked = resultSet.getInt("blocked") == 1;
        int failedAttempts = resultSet.getInt("failed_attempts");
        LocalDateTime createdAt = LocalDateTime.parse(resultSet.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(resultSet.getString("updated_at"));

        return new Account(id, accountNumber, pin, balance, dailyWithdrawalLimit,
                totalWithdrawnToday, blocked, failedAttempts, createdAt, updatedAt);
    }

    private void carregarTransacoes(Connection connection, Account account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TRANSACTIONS_BY_ACCOUNT)) {
            statement.setString(1, account.getId().toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID transactionId = UUID.fromString(resultSet.getString("id"));
                    TransactionType type = TransactionType.valueOf(resultSet.getString("type"));
                    BigDecimal amount = new BigDecimal(resultSet.getString("amount"));
                    String description = resultSet.getString("description");
                    LocalDateTime timestamp = LocalDateTime.parse(resultSet.getString("created_at"));

                    account.seedTransaction(
                            new Transaction(transactionId, timestamp, type, Money.of(amount), description));
                }
            }
        }
    }
}
