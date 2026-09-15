package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.BaseEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de repositório genérica e padronizada para todo o domínio
 * (Fase 3 - Paradigma Funcional e Generics).
 *
 * O limite superior {@code T extends BaseEntity} garante, em tempo de
 * compilação, que só entidades legítimas do domínio (que possuem id, data de
 * criação/atualização) possam ser persistidas por uma implementação deste
 * contrato.
 *
 * Todas as buscas retornam {@link Optional}: nenhuma implementação deste
 * repositório pode devolver {@code null} literal (Regra 5 - Uso Defensivo e
 * Erradicação do Null).
 */
public interface ATMRepository<T extends BaseEntity> {

    Optional<T> buscarPorId(UUID id);

    void salvar(T entidade);

    void remover(UUID id);

    List<T> buscarTodos();
}
