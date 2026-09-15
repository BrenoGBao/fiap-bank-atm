package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Account;
import java.util.Optional;

/**
 * Contrato de persistência específico do agregado Account.
 * Estende {@link ATMRepository} para herdar as operações genéricas
 * (buscarPorId, salvar, remover, buscarTodos) e acrescenta apenas a busca
 * que é peculiar de conta bancária: localizar pelo número da conta
 * (o "PIN de entrada" do caixa eletrônico).
 */
public interface AccountRepository extends ATMRepository<Account> {

    Optional<Account> buscarPorNumeroConta(String accountNumber);
}
