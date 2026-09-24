package br.com.sindico.app.anexo;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Grava anexos no Supabase Storage (bucket privado) via REST API.
 * Usa a service_role key, portanto so deve rodar no back-end — nunca expor ao front.
 * Referencia persistida: {@code supabase:{bucket}/{chave}}.
 */
public class SupabaseArquivoStorage implements ArquivoStorage {

    static final String PREFIXO = "supabase:";
    private static final Logger log = LoggerFactory.getLogger(SupabaseArquivoStorage.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;

    public SupabaseArquivoStorage(String supabaseUrl, String serviceKey, String bucket) {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), supabaseUrl, serviceKey, bucket);
    }

    SupabaseArquivoStorage(HttpClient httpClient, String supabaseUrl, String serviceKey, String bucket) {
        if (isBlank(supabaseUrl) || isBlank(serviceKey) || isBlank(bucket)) {
            throw new IllegalStateException(
                    "app.storage.provider=supabase exige SUPABASE_URL, SUPABASE_SERVICE_KEY e SUPABASE_STORAGE_BUCKET");
        }
        this.httpClient = httpClient;
        this.baseUrl = supabaseUrl.replaceAll("/+$", "");
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    @Override
    public String salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType) {
        validarChave(chave);
        HttpRequest request = autenticado(objetoUri(chave))
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.fromPublisher(
                        HttpRequest.BodyPublishers.ofInputStream(() -> conteudo), tamanhoBytes))
                .build();
        HttpResponse<String> response = enviar(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.error("Supabase Storage upload falhou: status={} body={}", response.statusCode(), response.body());
            throw new IllegalStateException("Falha ao gravar arquivo");
        }
        return PREFIXO + bucket + "/" + chave;
    }

    @Override
    public Resource carregar(String referencia) {
        if (referencia == null || !referencia.startsWith(PREFIXO + bucket + "/")) {
            // Anexos antigos gravados em disco local nao existem mais no host atual.
            throw new EntityNotFoundException("Arquivo do anexo nao encontrado.");
        }
        String chave = referencia.substring((PREFIXO + bucket + "/").length());
        validarChave(chave);
        HttpRequest request = autenticado(objetoUri(chave)).GET().build();
        HttpResponse<byte[]> response = enviar(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404 || response.statusCode() == 400) {
            throw new EntityNotFoundException("Arquivo do anexo nao encontrado.");
        }
        if (response.statusCode() / 100 != 2) {
            log.error("Supabase Storage download falhou: status={}", response.statusCode());
            throw new IllegalStateException("Falha ao ler arquivo");
        }
        return new ByteArrayResource(response.body());
    }

    private URI objetoUri(String chave) {
        return URI.create(baseUrl + "/storage/v1/object/" + bucket + "/" + chave);
    }

    private HttpRequest.Builder autenticado(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey);
    }

    private <T> HttpResponse<T> enviar(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return httpClient.send(request, handler);
        } catch (IOException e) {
            throw new IllegalStateException("Falha de comunicacao com o Supabase Storage", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operacao de storage interrompida", e);
        }
    }

    private static void validarChave(String chave) {
        if (chave == null || chave.isBlank() || chave.contains("..") || chave.startsWith("/")
                || !chave.matches("[a-zA-Z0-9._/-]+")) {
            throw new IllegalArgumentException("Caminho de arquivo invalido");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
