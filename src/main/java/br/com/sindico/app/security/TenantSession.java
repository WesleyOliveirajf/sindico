package br.com.sindico.app.security;

/** Chave de sessao HttpSession para condominio ativo (validado contra usuario logado). */
public final class TenantSession {

    public static final String CONDOMINIO_ID = "tenant.condominioId";

    private TenantSession() {}
}
