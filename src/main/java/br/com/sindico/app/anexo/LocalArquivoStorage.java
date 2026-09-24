package br.com.sindico.app.anexo;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * Grava anexos no disco local. Adequado para desenvolvimento; em hosts com disco efemero
 * (Render free, containers) os arquivos se perdem a cada deploy — use o provider supabase.
 */
public class LocalArquivoStorage implements ArquivoStorage {

    private final Path uploadRoot;

    public LocalArquivoStorage(String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType) {
        Path target = uploadRoot.resolve(chave).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Caminho de arquivo invalido");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(conteudo, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar arquivo", e);
        }
        return target.toString();
    }

    @Override
    public Resource carregar(String referencia) {
        Path filePath = Paths.get(referencia).toAbsolutePath().normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Arquivo fora do diretorio permitido");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("Arquivo do anexo nao encontrado.");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo", ex);
        }
    }
}
