package br.com.sindico.app.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class GoogleIdentityVerifierTest {

    private static String tokenForjado(String payloadJson) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
                + enc.encodeToString(payloadJson.getBytes()) + ".";
    }

    @Test
    void semClientIdRejeitaQualquerToken() {
        var verifier = new GoogleIdentityVerifier("");
        assertThrows(GoogleIdentityVerifier.TokenInvalidoException.class,
                () -> verifier.verificar(tokenForjado("{\"email\":\"a@b.com\"}")));
    }

    @Test
    void tokenSemAssinaturaValidaNaoEhAceito() {
        var verifier = new GoogleIdentityVerifier("cliente.apps.googleusercontent.com");
        assertThrows(GoogleIdentityVerifier.TokenInvalidoException.class,
                () -> verifier.verificar(tokenForjado("{\"email\":\"a@b.com\",\"name\":\"X\"}")));
    }

    @Test
    void tokenMalformadoNaoEhAceito() {
        var verifier = new GoogleIdentityVerifier("cliente.apps.googleusercontent.com");
        assertThrows(GoogleIdentityVerifier.TokenInvalidoException.class, () -> verifier.verificar("lixo"));
    }
}
