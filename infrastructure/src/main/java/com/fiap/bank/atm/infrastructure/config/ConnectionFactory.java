package com.fiap.bank.atm.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de conexões com o banco de dados SQLite (Fase 4 - Persistência de
 * Dados). Centraliza a URL de conexão e a criação do schema (DDL), para que
 * nenhuma outra classe do projeto precise conhecer detalhes de configuração
 * do banco.
 *
 * Acesso 100% nativo via JDBC puro (java.sql.*), sem nenhum ORM.
 */
public final class ConnectionFactory {

    private static final String DB_URL = "jdbc:sqlite:fiapbank.db";

    private ConnectionFactory() {
    }

    /**
     * Fornece uma nova conexão com o banco. O chamador é responsável por
     * fechá-la (idealmente com try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        // Habilita o suporte a FOREIGN KEY (desligado por padrão no SQLite),
        // necessário para o ON DELETE CASCADE entre tb_account e tb_transaction.
        try (Statement pragma = connection.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Cria as tabelas do sistema caso ainda não existam. Idempotente: pode
     * ser chamado toda vez que a aplicação sobe sem duplicar nada.
     */
    public static void initializeSchema() {
        // Observação sobre os tipos: colunas monetárias usam VARCHAR (não
        // NUMERIC/DECIMAL) de propósito. O SQLite tem "type affinity" dinâmica -
        // uma coluna NUMERIC/DECIMAL pode ser silenciosamente convertida para
        // REAL (ponto flutuante) internamente, o que arriscaria perder centavos
        // de precisão num sistema bancário. Guardando o valor com afinidade TEXT
        // (via VARCHAR) e lendo/gravando sempre como BigDecimal.toPlainString(),
        // garantimos precisão decimal exata de ponta a ponta.
        String createAccountTable = """
                CREATE TABLE IF NOT EXISTS tb_account (
                    id TEXT PRIMARY KEY,
                    account_number TEXT NOT NULL UNIQUE,
                    pin TEXT NOT NULL,
                    balance VARCHAR(20) NOT NULL,
                    daily_withdrawal_limit VARCHAR(20) NOT NULL,
                    total_withdrawn_today VARCHAR(20) NOT NULL,
                    blocked INTEGER NOT NULL,
                    failed_attempts INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """;

        String createTransactionTable = """
                CREATE TABLE IF NOT EXISTS tb_transaction (
                    id TEXT PRIMARY KEY,
                    account_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount VARCHAR(20) NOT NULL,
                    description VARCHAR(255),
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES tb_account(id) ON DELETE CASCADE
                )
                """;

        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(createAccountTable);
            statement.execute(createTransactionTable);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inicializar o schema do banco de dados.", e);
        }
    }
}
