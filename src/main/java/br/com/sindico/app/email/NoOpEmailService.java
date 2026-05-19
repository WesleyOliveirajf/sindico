package br.com.sindico.app.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * No-op: loga o link no console. Substitua por impl real quando tiver provedor.
 *
 * TODO: implementar com Spring Mail + SendGrid/Mailgun/SES.
 *       Adicionar dependencia spring-boot-starter-mail ao pom.xml,
 *       configurar spring.mail.* no application.yml e criar impl real.
 */
@Service
public class NoOpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void enviarResetSenha(String destinatario, String nomeUsuario, String linkReset) {
        log.warn(
                "[EMAIL NO-OP] Reset de senha solicitado para {} <{}>. Link suprimido por seguranca.",
                nomeUsuario, destinatario);
    }
}
