package br.com.sindico.app.anexo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupabaseArquivoStorageTest {

    private static final String CHAVE = "cond/manutencao/ent/arquivo.pdf";

    private HttpServer server;
    private final Map<String, byte[]> objetos = new HashMap<>();
    private final Map<String, String> ultimosHeaders = new HashMap<>();
    private SupabaseArquivoStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/storage/v1/object/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            ultimosHeaders.put("Authorization", exchange.getRequestHeaders().getFirst("Authorization"));
            ultimosHeaders.put("Content-Type", exchange.getRequestHeaders().getFirst("Content-Type"));
            if ("POST".equals(exchange.getRequestMethod())) {
                objetos.put(path, exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(200, -1);
            } else if (objetos.containsKey(path)) {
                byte[] body = objetos.get(path);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        });
        server.start();
        storage = new SupabaseArquivoStorage("http://127.0.0.1:" + server.getAddress().getPort() + "/", "chave-servico", "anexos");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void salvaEBaixaArquivoNoBucket() throws IOException {
        byte[] conteudo = "pdf-fake".getBytes(StandardCharsets.UTF_8);

        String referencia = storage.salvar(CHAVE, new ByteArrayInputStream(conteudo), conteudo.length, "application/pdf");

        assertThat(referencia).isEqualTo("supabase:anexos/" + CHAVE);
        assertThat(objetos).containsKey("/storage/v1/object/anexos/" + CHAVE);
        assertThat(ultimosHeaders.get("Authorization")).isEqualTo("Bearer chave-servico");
        assertThat(ultimosHeaders.get("Content-Type")).isEqualTo("application/pdf");
        assertThat(storage.carregar(referencia).getContentAsByteArray()).isEqualTo(conteudo);
    }

    @Test
    void arquivoInexistenteNoBucketGeraNotFound() {
        assertThatThrownBy(() -> storage.carregar("supabase:anexos/" + CHAVE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void referenciaLegadaDeDiscoGeraNotFound() {
        assertThatThrownBy(() -> storage.carregar("/app/uploads/cond/manutencao/ent/arquivo.pdf"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejeitaPathTraversal() {
        assertThatThrownBy(() -> storage.carregar("supabase:anexos/../outro-bucket/x.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.salvar("../x.pdf", new ByteArrayInputStream(new byte[0]), 0, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeConfiguracaoCompleta() {
        assertThatThrownBy(() -> new SupabaseArquivoStorage("", "k", "anexos"))
                .isInstanceOf(IllegalStateException.class);
    }
}
