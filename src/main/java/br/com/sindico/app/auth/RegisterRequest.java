package br.com.sindico.app.auth;

public record RegisterRequest(
        String nome,
        String email,
        String nomeCondominio,
        String senha,
        String confirmarSenha,
        Boolean aceitouTermos,
        Boolean aceitouMarketing
) {}
