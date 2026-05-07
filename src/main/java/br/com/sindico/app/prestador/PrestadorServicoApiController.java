package br.com.sindico.app.prestador;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestadores")
public class PrestadorServicoApiController {

    private final PrestadorServicoService prestadorServicoService;

    public PrestadorServicoApiController(PrestadorServicoService prestadorServicoService) {
        this.prestadorServicoService = prestadorServicoService;
    }

    @GetMapping
    public List<PrestadorServicoResponse> listarAtivos() {
        return prestadorServicoService.listarDoCondominioAtual()
                .stream()
                .map(PrestadorServicoResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<PrestadorServicoResponse> criar(@Valid @RequestBody PrestadorServicoRequest request) {
        NovoPrestadorForm form = new NovoPrestadorForm();
        form.setNome(request.nome());
        form.setTelefone(request.telefone());
        form.setHistoricoServicos(request.historicoServicos());

        PrestadorServico prestador = prestadorServicoService.criar(form);
        PrestadorServicoResponse response = PrestadorServicoResponse.from(prestador);

        return ResponseEntity
                .created(URI.create("/api/prestadores/" + response.id()))
                .body(response);
    }

    @PutMapping("/{prestadorId}")
    public ResponseEntity<?> atualizar(
            @PathVariable UUID prestadorId,
            @Valid @RequestBody PrestadorServicoRequest request) {
        try {
            AtualizarPrestadorForm form = new AtualizarPrestadorForm();
            form.setNome(request.nome());
            form.setTelefone(request.telefone());
            form.setHistoricoServicos(request.historicoServicos());

            PrestadorServico prestador = prestadorServicoService.atualizar(prestadorId, form);
            return ResponseEntity.ok(PrestadorServicoResponse.from(prestador));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{prestadorId}/inativar")
    public ResponseEntity<?> inativar(@PathVariable UUID prestadorId) {
        try {
            prestadorServicoService.inativar(prestadorId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarValidacao(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toError)
                .toList();

        return Map.of(
                "message", "Dados invalidos",
                "errors", errors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarRegraNegocio(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }

    private Map<String, String> toError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() == null ? "Valor invalido" : fieldError.getDefaultMessage()
        );
    }
}
