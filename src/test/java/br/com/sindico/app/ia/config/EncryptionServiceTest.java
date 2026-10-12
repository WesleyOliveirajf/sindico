package br.com.sindico.app.ia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    private static final String CHAVE = "chave-de-teste-com-32-caracteres!";

    private final EncryptionService service = new EncryptionService(CHAVE);

    @Test
    void descriptografaExatamenteOQueFoiCriptografado() {
        String cifrado = service.encrypt("sk-chave-secreta-123");

        assertThat(service.decrypt(cifrado)).isEqualTo("sk-chave-secreta-123");
    }

    @Test
    void textoCifradoNaoContemOTextoOriginal() {
        String cifrado = service.encrypt("sk-chave-secreta-123");

        assertThat(cifrado).doesNotContain("sk-chave-secreta-123");
    }

    @Test
    void mesmoTextoGeraCifradosDiferentesPorCausaDoIvAleatorio() {
        assertThat(service.encrypt("abc")).isNotEqualTo(service.encrypt("abc"));
    }

    @Test
    void suportaTextoComAcentosEEmoji() {
        assertThat(service.decrypt(service.encrypt("çãõ-ñ-😀"))).isEqualTo("çãõ-ñ-😀");
    }

    @Test
    void falhaAoDescriptografarComChaveDiferente() {
        String cifrado = service.encrypt("segredo");
        EncryptionService outraChave = new EncryptionService("outra-chave-de-teste-32-caracter");

        assertThatThrownBy(() -> outraChave.decrypt(cifrado))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("descriptografar");
    }

    @Test
    void falhaAoDescriptografarConteudoAdulterado() {
        String cifrado = service.encrypt("segredo");
        char ultimo = cifrado.charAt(cifrado.length() - 5);
        String adulterado = cifrado.substring(0, cifrado.length() - 5)
                + (ultimo == 'A' ? 'B' : 'A') + cifrado.substring(cifrado.length() - 4);

        assertThatThrownBy(() -> service.decrypt(adulterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaAoDescriptografarBase64Invalido() {
        assertThatThrownBy(() -> service.decrypt("%%%nao-e-base64%%%"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaAoCriptografarTextoNulo() {
        assertThatThrownBy(() -> service.encrypt(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("criptografar");
    }
}
