package br.com.sindico.app.security;

import java.util.UUID;

/**
 * Condominio autenticado na sessao (um por login; multi-condominio no mesmo usuario ainda usa o primeiro ordenado pelo nome).
 */
public interface TenantAccessor {

    UUID condominioAtual();
}
