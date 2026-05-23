package br.com.sindico.app.morador;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MoradorApiController {

    private final MoradorGestaoService moradorGestaoService;

    public MoradorApiController(MoradorGestaoService moradorGestaoService) {
        this.moradorGestaoService = moradorGestaoService;
    }

    @GetMapping("/unidades")
    public List<UnidadeResponse> listarUnidades() {
        return moradorGestaoService.listarUnidades().stream()
                .map(UnidadeResponse::from)
                .toList();
    }

    @PostMapping("/unidades")
    public ResponseEntity<UnidadeResponse> criarUnidade(@Valid @RequestBody UnidadeRequest request) {
        NovaUnidadeForm form = new NovaUnidadeForm();
        form.setBloco(request.bloco());
        form.setNumero(request.numero());
        form.setComplemento(request.complemento());
        moradorGestaoService.criarUnidade(form);
        // Fetch the created unit via listarUnidades (ordered) — last created matches bloco+numero
        Unidade criada = moradorGestaoService.listarUnidades().stream()
                .filter(u -> u.getNumero().equals(request.numero().trim())
                        && normalize(u.getBloco()).equals(normalize(request.bloco())))
                .findFirst()
                .orElseThrow();
        return ResponseEntity
                .created(URI.create("/api/unidades/" + criada.getId()))
                .body(UnidadeResponse.from(criada));
    }

    @GetMapping("/moradores")
    public List<MoradorResponse> listarMoradores() {
        return moradorGestaoService.listarMoradoresAtivos().stream()
                .map(MoradorResponse::from)
                .toList();
    }

    @PostMapping("/moradores")
    public ResponseEntity<MoradorResponse> criarMorador(@Valid @RequestBody MoradorRequest request) {
        NovoMoradorForm form = new NovoMoradorForm();
        form.setUnidadeId(request.unidadeId());
        form.setNome(request.nome());
        form.setEmail(request.email());
        form.setTelefone(request.telefone());
        form.setPapel(request.papel());
        form.setObservacoes(request.observacoes());
        moradorGestaoService.criarMorador(form);
        // Find the newly created morador by nome+unidade in current tenant
        Morador criado = moradorGestaoService.listarMoradoresAtivos().stream()
                .filter(m -> m.getNome().equalsIgnoreCase(request.nome().trim())
                        && m.getUnidade().getId().equals(request.unidadeId()))
                .findFirst()
                .orElseThrow();
        return ResponseEntity
                .created(URI.create("/api/moradores/" + criado.getId()))
                .body(MoradorResponse.from(criado));
    }

    @PutMapping("/moradores/{id}")
    public MoradorResponse atualizarMorador(@PathVariable UUID id, @Valid @RequestBody MoradorRequest request) {
        return MoradorResponse.from(moradorGestaoService.atualizarMorador(id, request));
    }

    @PostMapping("/moradores/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativarMorador(@PathVariable UUID id) {
        moradorGestaoService.inativarMorador(id);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
