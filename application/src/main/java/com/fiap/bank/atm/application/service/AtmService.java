package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.domain.exception.InvalidPinException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço de aplicação (fachada) do caixa eletrônico.
 *
 * Fase 2 do enunciado: todo método PENSADO PARA SER NOVO (autenticação,
 * saque, depósito, transferência, saldo e extrato) recebe e devolve apenas
 * DTOs (Records) ou tipos primitivos/wrapper - nunca a entidade {@code Account}
 * ou o Value Object {@code Money}.
 *
 * A única exceção documentada é {@link #getCurrentAccount()}: a tela Swing
 * legada ({@code AtmFrame.java}, que a Regra 1 do enunciado proíbe alterar)
 * já declara variáveis locais do tipo {@code Account} e chama diretamente
 * {@code getAccountNumber()}, {@code getBalance()}, {@code getTransactions()}
 * etc. para montar a tela de saldo e o comprovante de extrato. Trocar essa
 * assinatura quebraria a compilação da tela. Optou-se por manter esse único
 * método "vazando" a entidade - e documentá-lo aqui e no README - em vez de
 * violar a regra, ainda mais crítica, de não tocar no frontend.
 */
public class AtmService {
    private final AccountRepository accountRepository;
    private Account currentAccount;

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.buscarPorNumeroConta(accountNumber)
                .orElseThrow(() -> new InvalidPinException("Conta não encontrada."));

        try {
            account.authenticate(pin);
            currentAccount = account;
            return AccountInfoDTO.from(account);
        } catch (RuntimeException e) {
            accountRepository.salvar(account); // persiste tentativas falhas / bloqueio
            throw e;
        }
    }

    public void withdraw(double amount) {
        ensureAuthenticated();
        currentAccount.withdraw(Money.of(amount));
        accountRepository.salvar(currentAccount);
    }

    public void deposit(double amount) {
        ensureAuthenticated();
        currentAccount.deposit(Money.of(amount));
        accountRepository.salvar(currentAccount);
    }

    public void transfer(String targetAccountNumber, double amount) {
        ensureAuthenticated();

        Account targetAccount = accountRepository.buscarPorNumeroConta(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        currentAccount.transfer(targetAccount, Money.of(amount));

        accountRepository.salvar(currentAccount);
        accountRepository.salvar(targetAccount);
    }

    public BigDecimal getBalance() {
        ensureAuthenticated();
        return currentAccount.getBalance().getAmount();
    }

    public List<TransactionDTO> getStatement() {
        ensureAuthenticated();
        // Streams API no lugar do for clássico (Fase 3 - Modernização de Iterações)
        return currentAccount.getTransactions().stream()
                .map(TransactionDTO::from)
                .toList();
    }

    public void logout() {
        currentAccount = null;
    }

    /**
     * @deprecated Mantido apenas por compatibilidade com {@code AtmFrame.java}
     *             (ver javadoc da classe). Não use este método para código
     *             novo - prefira {@link #authenticate}, {@link #getBalance}
     *             ou {@link #getStatement}, que já expõem apenas DTOs.
     */
    @Deprecated
    public Account getCurrentAccount() {
        return currentAccount;
    }

    public boolean isAuthenticated() {
        return currentAccount != null;
    }

    private void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário está autenticado no momento.");
        }
    }
}
