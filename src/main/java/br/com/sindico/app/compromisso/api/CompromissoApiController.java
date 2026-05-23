package br.com.sindico.app.compromisso.api;

import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoService;
import br.com.sindico.app.compromisso.NovoCompromissoForm;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compromissos")
public class CompromissoApiController {

    private final CompromissoService compromissoService;

    public CompromissoApiController(CompromissoService compromissoService) {
        this.compromissoService = compromissoService;
    }

    /** Retorna TODOS os compromissos do condomínio (abertos e concluídos). */
    @GetMapping
    public List<CompromissoResponse> listar() {
        return compromissoService.listar().stream().map(CompromissoResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CompromissoResponse> criar(@Valid @RequestBody CompromissoRequest request) {
        NovoCompromissoForm form = new NovoCompromissoForm();
        form.setTitulo(request.titulo());
        form.setDescricao(request.descricao());
        form.setTipo(request.tipo());
        form.setInicioEm(request.inicioEm().atStartOfDay());
        form.setFimEm(null); // preenchido automaticamente ao concluir
        form.setLocal(request.local());

        Compromisso compromisso = compromissoService.criar(form);
        CompromissoResponse response = CompromissoResponse.from(compromisso);

        return ResponseEntity
                .created(URI.create("/api/compromissos/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public CompromissoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CompromissoRequest request) {
        NovoCompromissoForm form = new NovoCompromissoForm();
        form.setTitulo(request.titulo());
        form.setDescricao(request.descricao());
        form.setTipo(request.tipo());
        form.setInicioEm(request.inicioEm().atStartOfDay());
        form.setLocal(request.local());

        return CompromissoResponse.from(compromissoService.atualizar(id, form));
    }

    /**
     * Marca o compromisso como CONCLUIDO e registra fimEm = agora.
     */
    @PatchMapping("/{id}/concluir")
    public CompromissoResponse concluir(@PathVariable UUID id) {
        return CompromissoResponse.from(compromissoService.concluir(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        compromissoService.deletar(id);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> tratarNaoEncontrado(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarValidacao(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toError)
                .toList();
        return Map.of("message", "Dados invalidos", "errors", errors);
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
