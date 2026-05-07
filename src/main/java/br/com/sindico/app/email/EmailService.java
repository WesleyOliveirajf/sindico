package br.com.sindico.app.email;

/**
 * Contrato de envio de email. Implementacao padrao e no-op (loga no console).
 * Substitua por SendGrid / Mailgun / SES quando pronto.
 */
public interface EmailService {

    /**
     * Envia email de reset de senha.
     *
     * @param destinatario email do usuario
     * @param nomeUsuario  nome de exibicao
     * @param linkReset    URL completa com token (ex: https://app.com/redefinir-senha?token=xxx)
     */
    void enviarResetSenha(String destinatario, String nomeUsuario, String linkReset);
}
