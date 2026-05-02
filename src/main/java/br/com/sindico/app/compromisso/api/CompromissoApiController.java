package br.com.sindico.app.compromisso.api;

import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoService;
import br.com.sindico.app.compromisso.NovoCompromissoForm;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping
    public List<CompromissoResponse> listarProximos() {
        return compromissoService.proximos().stream().map(CompromissoResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CompromissoResponse> criar(@Valid @RequestBody CompromissoRequest request) {
        NovoCompromissoForm form = new NovoCompromissoForm();
        form.setTitulo(request.titulo());
        form.setDescricao(request.descricao());
        form.setTipo(request.tipo());
        form.setInicioEm(request.inicioEm());
        form.setFimEm(request.fimEm());
        form.setLocal(request.local());

        Compromisso compromisso = compromissoService.criar(form);
        CompromissoResponse response = CompromissoResponse.from(compromisso);

        return ResponseEntity
                .created(URI.create("/api/compromissos/" + response.id()))
                .body(response);
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
