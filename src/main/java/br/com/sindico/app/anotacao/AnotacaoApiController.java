package br.com.sindico.app.anotacao;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anotacoes")
public class AnotacaoApiController {

    private final AnotacaoService anotacaoService;

    public AnotacaoApiController(AnotacaoService anotacaoService) {
        this.anotacaoService = anotacaoService;
    }

    @GetMapping
    public List<AnotacaoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        return anotacaoService.listarComFiltros(texto, dataInicio, dataFim).stream()
                .map(AnotacaoResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AnotacaoResponse> criar(@Valid @RequestBody AnotacaoRequest request) {
        NovaAnotacaoForm form = toForm(request);
        Anotacao anotacao = anotacaoService.criar(form);
        AnotacaoResponse response = AnotacaoResponse.from(anotacao);
        return ResponseEntity
                .created(URI.create("/api/anotacoes/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public AnotacaoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AnotacaoRequest request) {
        return AnotacaoResponse.from(anotacaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        anotacaoService.deletar(id);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> tratarNaoEncontrado(jakarta.persistence.EntityNotFoundException ex) {
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

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> tratarEstadoInvalido(IllegalStateException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarRegraNegocio(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }

    private Map<String, String> toError(FieldError fe) {
        return Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() == null ? "Valor invalido" : fe.getDefaultMessage()
        );
    }

    private NovaAnotacaoForm toForm(AnotacaoRequest req) {
        NovaAnotacaoForm form = new NovaAnotacaoForm();
        form.setTitulo(req.titulo());
        form.setCategoria(req.categoria());
        form.setDescricao(req.descricao());
        form.setReferencia(req.referencia());
        form.setDataReferencia(req.dataReferencia());
        form.setImportancia(req.importancia());
        return form;
    }
}
