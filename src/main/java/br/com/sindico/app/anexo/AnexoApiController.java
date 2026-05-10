package br.com.sindico.app.anexo;

import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/anexos")
public class AnexoApiController {

    private final AnexoService anexoService;

    public AnexoApiController(AnexoService anexoService) {
        this.anexoService = anexoService;
    }

    @GetMapping
    public List<AnexoResponse> listar(
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId) {
        return anexoService.listar(entidadeTipo, entidadeId);
    }

    @PostMapping
    public AnexoResponse upload(
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return anexoService.upload(entidadeTipo, entidadeId, arquivo);
    }

    @GetMapping("/{anexoId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID anexoId) {
        AnexoService.DownloadPayload payload = anexoService.download(anexoId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (payload.mimeType() != null && !payload.mimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(payload.mimeType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(payload.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(payload.resource());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> tratarNaoEncontrado(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarRegraNegocio(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }
}
