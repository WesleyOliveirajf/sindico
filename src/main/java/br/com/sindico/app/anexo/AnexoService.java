package br.com.sindico.app.anexo;

import br.com.sindico.app.manutencao.ManutencaoRepository;
import br.com.sindico.app.reuniao.ReuniaoRepository;
import br.com.sindico.app.security.TenantAccessor;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnexoService {

    private static final Set<String> ENTITY_TYPES = Set.of("MANUTENCAO", "REUNIAO");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final AnexoRepository anexoRepository;
    private final TenantAccessor tenantAccessor;
    private final ManutencaoRepository manutencaoRepository;
    private final ReuniaoRepository reuniaoRepository;
    private final Path uploadRoot;
    private final long maxFileSizeBytes;

    public AnexoService(
            AnexoRepository anexoRepository,
            TenantAccessor tenantAccessor,
            ManutencaoRepository manutencaoRepository,
            ReuniaoRepository reuniaoRepository,
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.anexoRepository = anexoRepository;
        this.tenantAccessor = tenantAccessor;
        this.manutencaoRepository = manutencaoRepository;
        this.reuniaoRepository = reuniaoRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Transactional(readOnly = true)
    public List<AnexoResponse> listar(String entidadeTipo, UUID entidadeId) {
        String tipo = normalizeEntityType(entidadeTipo);
        validarEntidadeDoCondominioAtual(tipo, entidadeId);
        return anexoRepository
                .findByCondominioIdAndEntidadeTipoAndEntidadeIdOrderByCreatedAtDesc(tenantAccessor.condominioAtual(), tipo, entidadeId)
                .stream()
                .map(AnexoResponse::from)
                .toList();
    }

    @Transactional
    public AnexoResponse upload(String entidadeTipo, UUID entidadeId, MultipartFile file) {
        String tipo = normalizeEntityType(entidadeTipo);
        validarEntidadeDoCondominioAtual(tipo, entidadeId);
        validarArquivo(file);

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String ext = extensionOf(originalName);
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        UUID condominioId = tenantAccessor.condominioAtual();
        Path folder = uploadRoot.resolve(condominioId.toString()).resolve(tipo.toLowerCase()).resolve(entidadeId.toString()).normalize();

        if (!folder.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Caminho de upload invalido");
        }

        Path target = folder.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Caminho de arquivo invalido");
        }

        try {
            Files.createDirectories(folder);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar arquivo", e);
        }

        Anexo anexo = new Anexo();
        anexo.setCondominioId(condominioId);
        anexo.setEntidadeTipo(tipo);
        anexo.setEntidadeId(entidadeId);
        anexo.setNomeArquivo(originalName);
        anexo.setUrlArquivo(target.toString());
        anexo.setMimeType(file.getContentType());
        anexo.setTamanhoBytes(file.getSize());
        anexo.setEnviadoPor(usuarioAtualId());

        return AnexoResponse.from(anexoRepository.save(anexo));
    }

    @Transactional(readOnly = true)
    public DownloadPayload download(UUID anexoId) {
        Anexo anexo = anexoRepository.findByIdAndCondominioId(anexoId, tenantAccessor.condominioAtual())
                .orElseThrow(() -> new EntityNotFoundException("Anexo nao encontrado."));

        Path filePath = Paths.get(anexo.getUrlArquivo()).toAbsolutePath().normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Arquivo fora do diretorio permitido");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("Arquivo do anexo nao encontrado.");
            }
            return new DownloadPayload(resource, anexo.getNomeArquivo(), anexo.getMimeType());
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo", ex);
        }
    }

    public record DownloadPayload(Resource resource, String fileName, String mimeType) {}

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatorio");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Arquivo excede tamanho maximo permitido");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de arquivo nao permitido");
        }
    }

    private String normalizeEntityType(String entidadeTipo) {
        if (entidadeTipo == null) {
            throw new IllegalArgumentException("Entidade obrigatoria");
        }
        String t = entidadeTipo.trim().toUpperCase();
        if (!ENTITY_TYPES.contains(t)) {
            throw new IllegalArgumentException("Entidade invalida");
        }
        return t;
    }

    private void validarEntidadeDoCondominioAtual(String tipo, UUID entidadeId) {
        UUID condominioId = tenantAccessor.condominioAtual();
        boolean exists;
        if ("MANUTENCAO".equals(tipo)) {
            exists = manutencaoRepository.findById(entidadeId).map(m -> m.getCondominioId().equals(condominioId)).orElse(false);
        } else {
            exists = reuniaoRepository.findById(entidadeId).map(r -> r.getCondominioId().equals(condominioId)).orElse(false);
        }
        if (!exists) {
            throw new EntityNotFoundException("Registro nao encontrado para o condominio atual.");
        }
    }

    private UUID usuarioAtualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return principal.getUsuarioId();
        }
        throw new IllegalStateException("Nao foi possivel identificar usuario autenticado");
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "arquivo";
        }
        String normalized = fileName.replace("\\", "/");
        String base = normalized.substring(normalized.lastIndexOf('/') + 1);
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return "";
        return fileName.substring(idx + 1).toLowerCase();
    }
}
