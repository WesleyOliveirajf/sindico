package br.com.sindico.app.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida o ID token do Google (assinatura, expiracao e audience) e devolve a identidade.
 * Falha fechada: sem {@code GOOGLE_CLIENT_ID} configurado nenhum token e aceito.
 */
@Component
public class GoogleIdentityVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityVerifier.class);

    public record GoogleIdentity(String email, String nome) {}

    /** Token invalido ou nao verificavel: o controller responde 401. */
    public static class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String message) {
            super(message);
        }
    }

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityVerifier(@Value("${app.google.client-id:${GOOGLE_CLIENT_ID:}}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("GOOGLE_CLIENT_ID nao configurado: login com Google desabilitado.");
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(List.of(clientId.trim()))
                    .build();
        }
    }

    public GoogleIdentity verificar(String credentialToken) {
        if (verifier == null) {
            throw new TokenInvalidoException("Login com Google indisponivel");
        }
        try {
            GoogleIdToken idToken = verifier.verify(credentialToken);
            if (idToken == null) {
                throw new TokenInvalidoException("Assinatura do token inválida");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new TokenInvalidoException("E-mail da conta Google nao verificado");
            }
            return new GoogleIdentity(payload.getEmail(), (String) payload.get("name"));
        } catch (TokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao verificar token do Google: {}", e.getMessage());
            throw new TokenInvalidoException("Assinatura do token inválida");
        }
    }
}
